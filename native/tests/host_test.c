#define _DARWIN_C_SOURCE
#define _POSIX_C_SOURCE 200809L
#include "../host/azora_studio_host.h"
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

static void complete(int64_t process, int status) {
    int result;
    const struct timespec interval = {0, 1000000};
    int attempts = 0;
    do {
        result = azs_process_poll(process);
        nanosleep(&interval, NULL);
        assert(++attempts < 10000 && "native child did not complete within probe deadline");
    } while (result == -1);
    assert(result == status);
}
int main(int argc, char **argv) {
    assert(argc == 3);
    assert(azs_abi_version() == 1);
    char root[4096]; snprintf(root, sizeof root, "%s/project", argv[1]);
    int64_t project = azs_project_open(root, 1); assert(project);
    assert(!azs_project_open(root, 1)); // create never overwrites an existing project.
    int64_t buffer = azs_buffer_new("func main() { }\n"); assert(buffer);
    assert(azs_project_write(project, "src/main.az", buffer));
    int64_t copy = azs_project_read(project, "src/main.az"); assert(copy);
    assert(!strcmp(azs_buffer_text(copy), "func main() { }\n"));
    assert(azs_buffer_byte(copy, -1) == -1);
    assert(azs_buffer_byte(copy, azs_buffer_size(copy)) == -1);
    int64_t unicode = azs_buffer_new("\xce\xb1" "B"); assert(unicode);
    int64_t edited = azs_buffer_splice_byte(unicode, 0, 2, 'A'); assert(edited);
    assert(!strcmp(azs_buffer_text(edited), "AB"));
    assert(!azs_buffer_splice_byte(unicode, 4, 0, 'A'));
    assert(!azs_buffer_splice_byte(unicode, 0, 0, 0));
    int64_t view = azs_buffer_slice(edited, 1, 2); assert(view);
    assert(!strcmp(azs_buffer_text(view), "B"));
    assert(azs_buffer_close(view));
    assert(azs_buffer_close(edited));
    assert(azs_buffer_close(unicode));
    assert(!azs_project_write(project, "../escape.az", buffer));
    assert(!azs_project_read(project, "/etc/passwd"));
    char link[4096]; snprintf(link, sizeof link, "%s/project/src/link.az", argv[1]);
    assert(symlink("main.az", link) == 0);
    assert(!azs_project_read(project, "src/link.az"));
    assert(!azs_project_write(project, "src/link.az", buffer));
    assert(azs_buffer_close(copy));
    assert(!azs_buffer_close(copy));
    int64_t newer = azs_buffer_new("new"); assert(newer != copy);
    assert(azs_buffer_size(copy) == -1);
    assert(azs_buffer_size(newer + ((int64_t)1 << 40)) == -1);
    assert(azs_buffer_close(newer));
    assert(azs_buffer_close(buffer));
    assert(azs_project_close(project));
    assert(!azs_project_close(project));
    assert(azs_live_handles() == 0);
    // Never start script/JAR compilers through the installed service endpoint.
    char script[4096]; snprintf(script, sizeof script, "%s/compiler.sh", argv[1]);
    FILE *file = fopen(script, "w"); assert(file);
    assert(fputs("#!/bin/sh\nexit 0\n", file) >= 0);
    assert(fclose(file) == 0); assert(chmod(script, 0700) == 0);
    assert(!azs_process_start(script, "", "", ""));
    int64_t process = azs_process_start(argv[2], "", "output", ""); assert(process);
    complete(process, 7);
    assert(!strcmp(azs_process_output(process), "native output\n"));
    assert(azs_process_close(process));
    process = azs_process_start(argv[2], "", "flood", ""); assert(process);
    complete(process, 0);
    assert(strlen(azs_process_output(process)) == 1024 * 1024);
    assert(azs_process_close(process));
    process = azs_process_start(argv[2], "", "wait", ""); assert(process);
    assert(azs_process_cancel(process));
    assert(azs_process_poll(process) == 137);
    assert(azs_process_close(process));
    assert(azs_live_handles() == 0);
    puts("native host persistence/process/ownership probes passed");
    return 0;
}
