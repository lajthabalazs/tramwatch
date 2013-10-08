#include <string.h>#include <stdlib.h>#include "trigger.h"void fdbuffer_init() { memset(&framebuffer, 0, sizeof (framebuffer)); memset(&diffbuffer, 0, sizeof(diffbuffer)); int i; for (i = 0; i<HISTSIZE - 1; i++) {  diffbuffer[i].cells = (int*) malloc(100*100); }}void fdbuffer_destroy() { // Free init mallocs here!}int store_frame(bitmap_t *bitmap, crop_t *crop) { framebp++; if (framebp > HISTSIZE) framebp = 0; int cw = crop->right - crop->left; int ch = crop->bottom - crop->top; if (framebuffer[framebp].pixels == NULL) {  framebuffer[framebp].pixels = (pixel_t*) malloc(cw * ch * 3);  framebuffer[framebp].width = cw;  framebuffer[framebp].height = ch; } if (framebuffer[framebp].width != cw || framebuffer[framebp].height != ch) {  void *e = realloc(framebuffer[framebp].pixels, cw * ch * 3);  framebuffer[framebp].width = cw; framebuffer[framebp].height = ch; } int i; for (i = 0; i < ch; i++) {  memcpy((void*)(framebuffer[framebp].pixels + i * cw), (void*)(bitmap->pixels + (crop->top + i) * bitmap->width + crop->left), 3 * cw); }   return framebp;}void write_frame(int fn, char *filename) { save_png_to_file(&framebuffer[fn], filename);}int calculate_diff(diff_t *d, int f1, int f2) { if (framebuffer[f1].width != framebuffer[f2].width || framebuffer[f1].height != framebuffer[f2].height) {  return -1; } int gw = (framebuffer[f1].width / GRIDSIZE) * GRIDSIZE; int gh = (framebuffer[f1].height / GRIDSIZE) * GRIDSIZE; d->sizex = gw; d->sizey = gh;  int i,j,k; for (i = 0; i < gh; i++) {  for (j = 0; j < gw; j += GRIDSIZE) {   d->cells[i * gw + j] = 0;   for (k = 0; k < GRIDSIZE; k++) {    pixel_t *p1 = pixel_at(&(framebuffer[f1]), j + k, i);    pixel_t *p2 = pixel_at(&(framebuffer[f2]), j + k, i);	d->cells[i * gw + j] += abs((int)p1->blue - (int)p2->blue) + abs((int)p1->green - (int)p2->green) + abs((int)p1->red - (int)p2->red);   }  } }  return 0;}int calculate_alldiffs(int fref) {  int i; for (i = 0; i++; i < HISTSIZE - 1) {  int fc = (fref + HISTSIZE - 1 - i) % HISTSIZE;  int e = calculate_diff(&(diffbuffer[i]), fref, fc);  if (e != 0) break; }  return i;}