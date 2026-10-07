#ifndef AZORA_STUDIO_HOST_H
#define AZORA_STUDIO_HOST_H
#include <stdint.h>

/* ABI 1: main-thread-only. Handles are positive generation-qualified IDs,
 * never pointers. Every successful open/new/start has exactly one close.
 * Strings returned by text/env/error are borrowed until the next mutation of
 * their owner (environment strings last for the process). No callback retains
 * Azora storage. Project paths must be relative and symlinks are refused. */
int32_t azs_abi_version(void);
const char *azs_environment(const char *name);
int32_t azs_environment_int(const char *name);
double azs_monotonic_seconds(void);
/* Seconds since the Unix epoch, for timestamps that outlive the process. */
double azs_wall_seconds(void);
/* Sets an environment variable that processes started afterwards inherit. */
int32_t azs_set_environment(const char *name, const char *value);
/* Copies the directory tree at absolute [from] into [to] (created as needed),
 * skipping hidden entries and build output. Refuses symlinks. 1 on success. */
int32_t azs_copy_tree(const char *from, const char *to);
int64_t azs_peak_memory_bytes(void);
const char *azs_error(void);
int32_t azs_live_handles(void);
int64_t azs_project_open(const char *root, int32_t create);
int32_t azs_project_close(int64_t project);
int64_t azs_buffer_new(const char *text);
int64_t azs_buffer_allocate(int32_t size);
int32_t azs_buffer_set_byte(int64_t buffer, int32_t offset, int32_t value);
int64_t azs_buffer_splice_byte(int64_t buffer, int32_t offset, int32_t remove, int32_t value);
int64_t azs_buffer_slice(int64_t buffer, int32_t start, int32_t end);
int32_t azs_buffer_truncate(int64_t buffer, int32_t size);
int64_t azs_buffer_clone(int64_t buffer);
int32_t azs_buffer_close(int64_t buffer);
int32_t azs_buffer_size(int64_t buffer);
int32_t azs_buffer_byte(int64_t buffer, int32_t offset);
const char *azs_buffer_text(int64_t buffer);
int64_t azs_project_read(int64_t project, const char *relative);
int32_t azs_project_write(int64_t project, const char *relative, int64_t buffer);
/* Lines of "d name" / "f name", directories first, hidden entries omitted. */
int64_t azs_project_list(int64_t project, const char *relative);
int32_t azs_project_make_dir(int64_t project, const char *relative);
int64_t azs_process_start(const char *program, const char *directory,
                         const char *first, const char *second);
int32_t azs_process_poll(int64_t process);
const char *azs_process_output(int64_t process);
int32_t azs_process_cancel(int64_t process);
int32_t azs_process_close(int64_t process);
void azs_diagnostic(int32_t offset, int32_t length, int32_t line,
                    int32_t column, const char *message);
void azs_symbol(int32_t offset, int32_t length, int32_t line, int32_t column);
void azs_exit(int32_t status);
void azs_wait_tick(void);
#endif
