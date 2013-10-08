#ifndef PNGHELPER
#define PNGHELPER

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

typedef struct {
    uint8_t blue;
    uint8_t green;
    uint8_t red;
} pixel_t; // In RV24 it lloks like to order is BGR and not RGB

/* A picture. */

typedef struct  {
    pixel_t *pixels;
    size_t width;
    size_t height;
} bitmap_t;

typedef struct {
    int top;
    int bottom;
    int left;
    int right;
} crop_t;

pixel_t * pixel_at (bitmap_t * bitmap, int x, int y);

int save_png_to_file (bitmap_t *bitmap, const char *path);
int save_png_to_file_crop (bitmap_t *bitmap, crop_t *crop, const char *path);

#endif
