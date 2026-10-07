#define _DARWIN_C_SOURCE
#define _POSIX_C_SOURCE 200809L
#define _XOPEN_SOURCE 700
#include "azora_studio_host.h"
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <sys/stat.h>
#include <sys/resource.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

enum { SLOTS = 128, MAX_SOURCE = 16 * 1024 * 1024, MAX_OUTPUT = 1024 * 1024 };
enum Kind { FREE, PROJECT, BUFFER, PROCESS };
typedef struct {
    enum Kind kind;
    uint32_t generation;
    char *text;
    size_t size, capacity;
    int fd;
    pid_t pid;
    int status, done;
} Slot;
static Slot slots[SLOTS];
static char last_error[256];
static unsigned temp_sequence;

static int fail(const char *message) {
    snprintf(last_error, sizeof last_error, "%s", message);
    return 0;
}
static int system_fail(const char *message) {
    snprintf(last_error, sizeof last_error, "%s: %s", message, strerror(errno));
    return 0;
}
static Slot *get(int64_t handle, enum Kind kind) {
    uint64_t id = (uint64_t)handle;
    unsigned index = (unsigned)(id & 255u);
    uint32_t generation = (uint32_t)(id >> 8);
    if (handle <= 0 || (id >> 8) > UINT32_MAX || index >= SLOTS || slots[index].kind != kind ||
        slots[index].generation != generation) {
        fail("invalid, stale or closed native handle");
        return NULL;
    }
    return &slots[index];
}
static int64_t allocate(enum Kind kind) {
    for (unsigned i = 0; i < SLOTS; ++i) {
        if (slots[i].kind != FREE) continue;
        uint32_t generation = slots[i].generation + 1;
        /* Retire a slot when its generation would wrap; stale IDs stay stale. */
        if (!generation) continue;
        memset(&slots[i], 0, sizeof slots[i]);
        slots[i].generation = generation;
        slots[i].kind = kind;
        slots[i].fd = -1;
        return (int64_t)(((uint64_t)generation << 8) | i);
    }
    fail("native handle capacity exhausted");
    return 0;
}
static void release(Slot *slot) {
    if (slot->fd >= 0) close(slot->fd);
    free(slot->text);
    slot->text = NULL;
    slot->fd = -1;
    slot->kind = FREE;
}

int32_t azs_abi_version(void) { return 1; }
const char *azs_environment(const char *name) {
    const char *value = getenv(name);
    return value ? value : "";
}
int32_t azs_environment_int(const char *name) {
    const char *text = getenv(name);
    if (!text || !*text) return 0;
    char *end;
    errno = 0;
    long value = strtol(text, &end, 10);
    return errno || *end || value < 0 || value > INT32_MAX ? 0 : (int32_t)value;
}
double azs_monotonic_seconds(void) {
    struct timespec time;
    if (clock_gettime(CLOCK_MONOTONIC, &time) < 0) return 0;
    return (double)time.tv_sec + (double)time.tv_nsec / 1000000000.0;
}
double azs_wall_seconds(void) {
    struct timespec time;
    if (clock_gettime(CLOCK_REALTIME, &time) < 0) return 0;
    return (double)time.tv_sec + (double)time.tv_nsec / 1000000000.0;
}
int32_t azs_set_environment(const char *name, const char *value) {
    if (!name || !*name || !value) { fail("invalid environment variable"); return 0; }
    if (setenv(name, value, 1) < 0) { system_fail("set environment variable"); return 0; }
    return 1;
}
static int copy_file(const char *from, const char *to, mode_t mode) {
    int in = open(from, O_RDONLY);
    if (in < 0) return 0;
    int out = open(to, O_WRONLY | O_CREAT | O_TRUNC, mode & 0777);
    if (out < 0) { close(in); return 0; }
    char bytes[65536];
    int ok = 1;
    for (;;) {
        ssize_t count = read(in, bytes, sizeof bytes);
        if (count < 0 && errno == EINTR) continue;
        if (count < 0) { ok = 0; break; }
        if (count == 0) break;
        for (ssize_t written = 0; written < count;) {
            ssize_t step = write(out, bytes + written, (size_t)(count - written));
            if (step < 0 && errno == EINTR) continue;
            if (step < 0) { ok = 0; break; }
            written += step;
        }
        if (!ok) break;
    }
    close(in);
    if (close(out) < 0) ok = 0;
    return ok;
}
static int copy_tree(const char *from, const char *to, int depth) {
    if (depth > 32) return 0;
    if (mkdir(to, 0755) < 0 && errno != EEXIST) return 0;
    DIR *dir = opendir(from);
    if (!dir) return 0;
    int ok = 1;
    struct dirent *entry;
    while (ok && (entry = readdir(dir))) {
        const char *name = entry->d_name;
        // Hidden entries (., .., .build, .azora-build, .DS_Store) and build output stay behind.
        if (name[0] == '.' || strcmp(name, "build") == 0) continue;
        char source[PATH_MAX], target[PATH_MAX];
        if (snprintf(source, sizeof source, "%s/%s", from, name) >= (int)sizeof source ||
            snprintf(target, sizeof target, "%s/%s", to, name) >= (int)sizeof target) { ok = 0; break; }
        struct stat info;
        if (lstat(source, &info) < 0 || S_ISLNK(info.st_mode)) { ok = 0; break; }
        if (S_ISDIR(info.st_mode)) ok = copy_tree(source, target, depth + 1);
        else if (S_ISREG(info.st_mode)) ok = copy_file(source, target, info.st_mode);
    }
    closedir(dir);
    return ok;
}
int32_t azs_copy_tree(const char *from, const char *to) {
    if (!from || from[0] != '/' || !to || to[0] != '/') { fail("copy needs absolute paths"); return 0; }
    if (!copy_tree(from, to, 0)) { system_fail("copy template"); return 0; }
    return 1;
}
int64_t azs_peak_memory_bytes(void) {
    struct rusage usage;
    if (getrusage(RUSAGE_SELF, &usage) < 0) return -1;
#ifdef __APPLE__
    return (int64_t)usage.ru_maxrss;
#else
    return (int64_t)usage.ru_maxrss * 1024;
#endif
}
const char *azs_error(void) { return last_error; }
int32_t azs_live_handles(void) {
    int count = 0;
    for (int i = 0; i < SLOTS; ++i) if (slots[i].kind != FREE) ++count;
    return count;
}

int64_t azs_project_open(const char *root, int32_t create) {
    if (!root || *root != '/') { fail("an absolute project directory is required"); return 0; }
    if (create && mkdir(root, 0700) < 0) {
        system_fail("create project"); return 0;
    }
    struct stat info;
    if (lstat(root, &info) < 0) { system_fail("open project"); return 0; }
    if (!S_ISDIR(info.st_mode)) { fail("project root must be a directory, not a symlink"); return 0; }
    int fd = open(root, O_RDONLY | O_DIRECTORY | O_NOFOLLOW);
    if (fd < 0) { system_fail("open project directory"); return 0; }
    int64_t handle = allocate(PROJECT);
    if (!handle) { close(fd); return 0; }
    Slot *slot = get(handle, PROJECT);
    slot->fd = fd;
    slot->text = realpath(root, NULL);
    if (!slot->text) { system_fail("canonical project directory"); release(slot); return 0; }
    if (create && mkdirat(fd, "src", 0700) < 0 && errno != EEXIST) {
        system_fail("create source directory"); release(slot); return 0;
    }
    return handle;
}
int32_t azs_project_close(int64_t handle) {
    Slot *slot = get(handle, PROJECT);
    if (!slot) return 0;
    release(slot); return 1;
}

int64_t azs_buffer_new(const char *text) {
    if (!text) { fail("source text is required"); return 0; }
    size_t size = strlen(text);
    if (size > MAX_SOURCE) { fail("source exceeds 16 MiB limit"); return 0; }
    int64_t handle = allocate(BUFFER);
    if (!handle) return 0;
    Slot *slot = get(handle, BUFFER);
    slot->text = malloc(size + 1);
    if (!slot->text) { fail("out of memory"); release(slot); return 0; }
    memcpy(slot->text, text, size + 1); slot->size = size;
    return handle;
}
int64_t azs_buffer_allocate(int32_t size) {
    if (size < 0 || size > MAX_SOURCE) { fail("invalid buffer size"); return 0; }
    int64_t handle = allocate(BUFFER);
    if (!handle) return 0;
    Slot *slot = get(handle, BUFFER);
    slot->text = calloc((size_t)size + 1, 1);
    if (!slot->text) { fail("out of memory"); release(slot); return 0; }
    slot->size = (size_t)size; return handle;
}
int32_t azs_buffer_set_byte(int64_t handle, int32_t offset, int32_t value) {
    Slot *slot = get(handle, BUFFER);
    if (!slot) return 0;
    if (offset < 0 || (size_t)offset >= slot->size || value < 0 || value > 255)
        return fail("invalid buffer byte store");
    slot->text[offset] = (char)value; return 1;
}
int64_t azs_buffer_splice_byte(int64_t handle, int32_t offset, int32_t remove, int32_t value) {
    Slot *source = get(handle, BUFFER);
    if (!source) return 0;
    if (offset < 0 || remove < 0 || (size_t)offset > source->size ||
        (size_t)remove > source->size - (size_t)offset || value < -1 || value == 0 || value > 127) {
        fail("invalid source edit range"); return 0;
    }
    size_t added = value < 0 ? 0 : 1;
    size_t size = source->size - (size_t)remove + added;
    if (size > MAX_SOURCE) { fail("edited source exceeds 16 MiB limit"); return 0; }
    int64_t next = azs_buffer_allocate((int32_t)size);
    if (!next) return 0;
    Slot *target = get(next, BUFFER);
    memcpy(target->text, source->text, (size_t)offset);
    if (added) target->text[offset] = (char)value;
    size_t tail = source->size - (size_t)offset - (size_t)remove;
    memcpy(target->text + offset + added, source->text + offset + remove, tail);
    return next;
}
int64_t azs_buffer_slice(int64_t handle, int32_t start, int32_t end) {
    Slot *source = get(handle, BUFFER);
    if (!source) return 0;
    if (start < 0 || end < start || (size_t)end > source->size) {
        fail("invalid source view range"); return 0;
    }
    int64_t slice = azs_buffer_allocate(end - start);
    if (!slice) return 0;
    Slot *target = get(slice, BUFFER);
    memcpy(target->text, source->text + start, (size_t)(end - start));
    return slice;
}
int32_t azs_buffer_truncate(int64_t handle, int32_t size) {
    Slot *source = get(handle, BUFFER);
    if (!source) return 0;
    if (size < 0 || (size_t)size > source->size) return fail("invalid buffer truncate size");
    source->size = (size_t)size; source->text[size] = 0; return 1;
}
int64_t azs_buffer_clone(int64_t handle) {
    Slot *slot = get(handle, BUFFER);
    return slot ? azs_buffer_new(slot->text) : 0;
}
int32_t azs_buffer_close(int64_t handle) {
    Slot *slot = get(handle, BUFFER);
    if (!slot) return 0;
    release(slot); return 1;
}
int32_t azs_buffer_size(int64_t handle) {
    Slot *slot = get(handle, BUFFER);
    return slot ? (int32_t)slot->size : -1;
}
int32_t azs_buffer_byte(int64_t handle, int32_t offset) {
    Slot *slot = get(handle, BUFFER);
    if (!slot) return -1;
    if (offset < 0 || (size_t)offset >= slot->size) return -1;
    return (unsigned char)slot->text[offset];
}
const char *azs_buffer_text(int64_t handle) {
    Slot *slot = get(handle, BUFFER);
    return slot ? slot->text : "";
}

/* Walk each parent component through directory descriptors. Refuse .., empty
 * components, absolute paths and symlinks, including a replaced project path. */
static int parent_fd(Slot *project, const char *relative, char name[NAME_MAX + 1]) {
    if (!relative || !*relative || *relative == '/' || strlen(relative) >= PATH_MAX) {
        fail("invalid relative project path"); return -1;
    }
    char path[PATH_MAX]; strcpy(path, relative);
    int directory = dup(project->fd);
    if (directory < 0) { system_fail("duplicate project directory"); return -1; }
    char *segment = path;
    for (;;) {
        char *slash = strchr(segment, '/');
        if (slash) *slash = 0;
        size_t size = strlen(segment);
        if (!size || size > NAME_MAX || !strcmp(segment, ".") || !strcmp(segment, "..")) {
            close(directory); fail("invalid relative project path"); return -1;
        }
        if (!slash) { strcpy(name, segment); return directory; }
        int next = openat(directory, segment, O_RDONLY | O_DIRECTORY | O_NOFOLLOW);
        close(directory);
        if (next < 0) { system_fail("open source parent"); return -1; }
        directory = next; segment = slash + 1;
    }
}
int64_t azs_project_read(int64_t handle, const char *relative) {
    Slot *project = get(handle, PROJECT);
    if (!project) return 0;
    char name[NAME_MAX + 1]; int parent = parent_fd(project, relative, name);
    if (parent < 0) return 0;
    int fd = openat(parent, name, O_RDONLY | O_NOFOLLOW | O_NONBLOCK);
    close(parent);
    if (fd < 0) { system_fail("open source"); return 0; }
    struct stat info;
    if (fstat(fd, &info) < 0 || !S_ISREG(info.st_mode) || info.st_size < 0 || info.st_size > MAX_SOURCE) {
        close(fd); fail("source must be a regular file of at most 16 MiB"); return 0;
    }
    size_t size = (size_t)info.st_size;
    char *text = malloc(size + 1);
    if (!text) { close(fd); fail("out of memory"); return 0; }
    size_t offset = 0;
    while (offset < size) {
        ssize_t count = read(fd, text + offset, size - offset);
        if (count < 0 && errno == EINTR) continue;
        if (count <= 0) { free(text); close(fd); fail("source changed while being read"); return 0; }
        offset += (size_t)count;
    }
    close(fd); text[size] = 0;
    if (memchr(text, 0, size)) { free(text); fail("embedded NUL is not valid source text"); return 0; }
    int64_t result = azs_buffer_new(text); free(text); return result;
}
int32_t azs_project_write(int64_t handle, const char *relative, int64_t buffer) {
    Slot *project = get(handle, PROJECT), *source = get(buffer, BUFFER);
    if (!project || !source) return 0;
    char name[NAME_MAX + 1]; int parent = parent_fd(project, relative, name);
    if (parent < 0) return 0;
    struct stat target;
    if (fstatat(parent, name, &target, AT_SYMLINK_NOFOLLOW) == 0 && !S_ISREG(target.st_mode)) {
        close(parent); return fail("save target must be a regular file, not a symlink");
    }
    char temporary[NAME_MAX + 1];
    snprintf(temporary, sizeof temporary, ".azora-save-%ld-%u", (long)getpid(), ++temp_sequence);
    int fd = openat(parent, temporary, O_WRONLY | O_CREAT | O_EXCL | O_NOFOLLOW, 0600);
    if (fd < 0) { close(parent); return system_fail("create atomic save"); }
    size_t offset = 0; int ok = 1;
    while (offset < source->size) {
        ssize_t count = write(fd, source->text + offset, source->size - offset);
        if (count < 0 && errno == EINTR) continue;
        if (count <= 0) { ok = system_fail("write source"); break; }
        offset += (size_t)count;
    }
    if (ok && fsync(fd) < 0) ok = system_fail("sync source");
    if (close(fd) < 0 && ok) ok = system_fail("close source");
    if (ok && renameat(parent, temporary, parent, name) < 0) ok = system_fail("commit source");
    if (ok && fsync(parent) < 0) ok = system_fail("sync project directory");
    if (!ok) unlinkat(parent, temporary, 0);
    close(parent); return ok;
}

/* Opens a project directory for listing: the root for "" or ".", otherwise a
 * relative path walked one segment at a time, refusing symlinks. */
static int directory_fd(Slot *project, const char *relative) {
    if (!relative || !*relative || !strcmp(relative, ".")) {
        int root = dup(project->fd);
        if (root < 0) system_fail("duplicate project directory");
        return root;
    }
    char name[NAME_MAX + 1]; int parent = parent_fd(project, relative, name);
    if (parent < 0) return -1;
    int fd = openat(parent, name, O_RDONLY | O_DIRECTORY | O_NOFOLLOW);
    close(parent);
    if (fd < 0) system_fail("open project directory");
    return fd;
}
static int compare_entries(const void *a, const void *b) {
    const char *left = *(const char *const *)a, *right = *(const char *const *)b;
    if (left[0] != right[0]) return left[0] == 'd' ? -1 : 1;
    return strcasecmp(left + 2, right + 2);
}
int64_t azs_project_list(int64_t handle, const char *relative) {
    Slot *project = get(handle, PROJECT);
    if (!project) return 0;
    int fd = directory_fd(project, relative);
    if (fd < 0) return 0;
    DIR *directory = fdopendir(fd);
    if (!directory) { close(fd); system_fail("list project directory"); return 0; }
    char **entries = NULL; size_t count = 0, capacity = 0, bytes = 1;
    struct dirent *entry;
    while ((entry = readdir(directory))) {
        if (entry->d_name[0] == '.') continue; /* hidden files, `.` and `..` */
        struct stat info;
        if (fstatat(dirfd(directory), entry->d_name, &info, AT_SYMLINK_NOFOLLOW) < 0) continue;
        if (!S_ISDIR(info.st_mode) && !S_ISREG(info.st_mode)) continue;
        if (count == capacity) {
            capacity = capacity ? capacity * 2 : 32;
            char **grown = realloc(entries, capacity * sizeof *entries);
            if (!grown) break;
            entries = grown;
        }
        size_t length = strlen(entry->d_name);
        entries[count] = malloc(length + 3);
        if (!entries[count]) break;
        entries[count][0] = S_ISDIR(info.st_mode) ? 'd' : 'f';
        entries[count][1] = ' ';
        memcpy(entries[count] + 2, entry->d_name, length + 1);
        bytes += length + 3;
        ++count;
    }
    closedir(directory);
    qsort(entries, count, sizeof *entries, compare_entries);
    char *text = malloc(bytes);
    if (!text) { for (size_t i = 0; i < count; ++i) free(entries[i]); free(entries); fail("out of memory"); return 0; }
    size_t offset = 0;
    for (size_t i = 0; i < count; ++i) {
        size_t length = strlen(entries[i]);
        memcpy(text + offset, entries[i], length);
        offset += length;
        text[offset++] = '\n';
        free(entries[i]);
    }
    text[offset] = 0;
    free(entries);
    int64_t result = azs_buffer_new(text); free(text); return result;
}
int32_t azs_project_make_dir(int64_t handle, const char *relative) {
    Slot *project = get(handle, PROJECT);
    if (!project) return 0;
    char name[NAME_MAX + 1]; int parent = parent_fd(project, relative, name);
    if (parent < 0) return 0;
    int ok = mkdirat(parent, name, 0700) == 0 || errno == EEXIST;
    if (!ok) system_fail("create project directory");
    close(parent); return ok;
}

static int native_program(const char *program) {
    if (!program || *program != '/') return fail("select an absolute native executable path");
    int fd = open(program, O_RDONLY | O_NOFOLLOW);
    if (fd < 0) return system_fail("open native executable");
    unsigned char magic[4]; struct stat info;
    int ok = fstat(fd, &info) == 0 && S_ISREG(info.st_mode) && read(fd, magic, 4) == 4;
    close(fd);
    if (!ok) return fail("native executable is unreadable");
    int elf = !memcmp(magic, "\177ELF", 4);
    int macho = (!memcmp(magic, "\xcf\xfa\xed\xfe", 4) || !memcmp(magic, "\xce\xfa\xed\xfe", 4) ||
                 !memcmp(magic, "\xfe\xed\xfa\xcf", 4) || !memcmp(magic, "\xfe\xed\xfa\xce", 4) ||
                 !memcmp(magic, "\xca\xfe\xba\xbe", 4) || !memcmp(magic, "\xbe\xba\xfe\xca", 4));
    if (!elf && !macho) return fail("Studio requires a native compiler/tool; script and JAR bootstraps are development-only");
    return access(program, X_OK) == 0 ? 1 : system_fail("native tool is not executable");
}
int64_t azs_process_start(const char *program, const char *directory, const char *first, const char *second) {
    if (!native_program(program)) return 0;
    int64_t handle = allocate(PROCESS);
    if (!handle) return 0;
    Slot *process = get(handle, PROCESS);
    process->capacity = MAX_OUTPUT + 1; process->text = calloc(process->capacity, 1);
    if (!process->text) { fail("out of memory"); release(process); return 0; }
    int pipefd[2];
    if (pipe(pipefd) < 0) { system_fail("create process pipe"); release(process); return 0; }
    pid_t pid = fork();
    if (pid < 0) { close(pipefd[0]); close(pipefd[1]); system_fail("start process"); release(process); return 0; }
    if (pid == 0) {
        if (setpgid(0, 0) < 0) _exit(126);
        close(pipefd[0]);
        if (dup2(pipefd[1], STDOUT_FILENO) < 0 || dup2(pipefd[1], STDERR_FILENO) < 0) _exit(126);
        close(pipefd[1]);
        for (int i = 0; i < SLOTS; ++i) if (slots[i].fd >= 0) close(slots[i].fd);
        if (directory && *directory && chdir(directory) < 0) _exit(126);
        char *args[4] = {(char *)program, NULL, NULL, NULL};
        if (first && *first) args[1] = (char *)first;
        if (second && *second) args[args[1] ? 2 : 1] = (char *)second;
        execv(program, args); _exit(127);
    }
    close(pipefd[1]); setpgid(pid, pid);
    if (fcntl(pipefd[0], F_SETFL, O_NONBLOCK) < 0) {
        kill(-pid, SIGKILL); waitpid(pid, NULL, 0); close(pipefd[0]);
        system_fail("configure process output"); release(process); return 0;
    }
    process->fd = pipefd[0]; process->pid = pid; return handle;
}
static void drain(Slot *process) {
    char bytes[4096];
    size_t drained = 0;
    for (;;) {
        ssize_t count = read(process->fd, bytes, sizeof bytes);
        if (count < 0 && errno == EINTR) continue;
        if (count <= 0) break;
        size_t copy = (size_t)count;
        if (copy > MAX_OUTPUT - process->size) copy = MAX_OUTPUT - process->size;
        memcpy(process->text + process->size, bytes, copy);
        process->size += copy; process->text[process->size] = 0;
        /* Continue draining after truncation so the child never blocks. */
        drained += (size_t)count;
        /* A continuously writing child must not monopolize the editor frame. */
        if (drained >= 65536) break;
    }
}
int32_t azs_process_poll(int64_t handle) {
    Slot *process = get(handle, PROCESS);
    if (!process) return -3;
    drain(process);
    if (!process->done) {
        int status; pid_t result = waitpid(process->pid, &status, WNOHANG);
        if (result < 0 && errno != EINTR) { system_fail("wait for process"); return -3; }
        if (result == 0 || result < 0) return -1;
        process->status = WIFEXITED(status) ? WEXITSTATUS(status) : 128 + WTERMSIG(status);
        process->done = 1; drain(process);
    }
    return process->status;
}
const char *azs_process_output(int64_t handle) {
    Slot *process = get(handle, PROCESS);
    return process ? process->text : "";
}
int32_t azs_process_cancel(int64_t handle) {
    Slot *process = get(handle, PROCESS);
    if (!process) return 0;
    if (!process->done) {
        /* A child can finish or fail its group setup between start and cancel.
         * Never block waitpid after an unsuccessful group signal. The direct
         * child remains ours and is the safe fallback target. */
        if (kill(-process->pid, SIGKILL) < 0 && kill(process->pid, SIGKILL) < 0 && errno != ESRCH)
            return system_fail("signal process cancellation");
        int status; pid_t result;
        do { result = waitpid(process->pid, &status, 0); } while (result < 0 && errno == EINTR);
        if (result < 0) return system_fail("cancel process");
        process->done = 1; process->status = 128 + SIGKILL; drain(process);
    }
    return 1;
}
int32_t azs_process_close(int64_t handle) {
    Slot *process = get(handle, PROCESS);
    if (!process) return 0;
    if (!process->done && !azs_process_cancel(handle)) return 0;
    release(process); return 1;
}
void azs_diagnostic(int32_t offset, int32_t length, int32_t line, int32_t column, const char *message) {
    printf("diagnostic\t%d\t%d\t%d\t%d\t%s\n", offset, length, line, column, message);
}
void azs_symbol(int32_t offset, int32_t length, int32_t line, int32_t column) {
    printf("symbol\t%d\t%d\t%d\t%d\n", offset, length, line, column);
}
void azs_exit(int32_t status) { exit(status); }
void azs_wait_tick(void) {
    const struct timespec tick = {0, 1000000};
    nanosleep(&tick, NULL);
}
