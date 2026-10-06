#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <string.h>
#include <unistd.h>
int main(int argc, char **argv) {
    if (argc < 2) return 9;
    if (!strcmp(argv[1], "output")) { puts("native output"); return 7; }
    if (!strcmp(argv[1], "wait")) { for (;;) pause(); }
    if (!strcmp(argv[1], "flood")) {
        char text[4096]; memset(text, 'x', sizeof text);
        for (int i = 0; i < 512; ++i) if (fwrite(text, 1, sizeof text, stdout) != sizeof text) return 1;
        return 0;
    }
    return 8;
}
