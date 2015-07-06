#include <png.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
	
#include "pnghelper.h"

#define max(a,b) \
   ({ __typeof__ (a) _a = (a); \
       __typeof__ (b) _b = (b); \
     _a > _b ? _a : _b; })

/* Given "bitmap", this returns the pixel of bitmap at the point 
   ("x", "y"). */

static pixel_t tmpp;
   
inline pixel_t * pixel_at (bitmap_t * bitmap, int x, int y)
{
    return bitmap->pixels + bitmap->width * y + x;
}

pixel_t * yellow_at(bitmap_t * bitmap, int x, int y) {
	memcpy(&tmpp, bitmap->pixels + bitmap->width * y + x, sizeof (pixel_t));
	// RGB to CYMK
	float k,yellow;
	k = 1.0 - (float)(max(max(tmpp.red,tmpp.green),tmpp.blue)) / 255.0;
	yellow = (1.0 - (float)tmpp.blue / 255.0 - k) / (1.0 - k);
	// Keep yellow, CYMK to RGB
	tmpp.red = 255.0 * (1.0 - 0.0) * (1.0 - k);
	tmpp.green = 255.0 * (1.0 - 0.0) * (1.0 - k);
	tmpp.blue = 255.0 * (1.0 - yellow) * (1.0 - k);
	
	return &tmpp;
}
    
/* Write "bitmap" to a PNG file specified by "path"; returns 0 on
   success, non-zero on error. */

int save_png_to_file (bitmap_t *bitmap, const char *path)
{
    return save_png_to_file_crop (bitmap, NULL, path);
}

int save_png_to_file_crop (bitmap_t *bitmap, crop_t *crop, const char *path)
{
    FILE * fp;
    png_structp png_ptr = NULL;
    png_infop info_ptr = NULL;
    size_t x, y;
    png_byte ** row_pointers = NULL;
    /* "status" contains the return value of this function. At first
       it is set to a value which means 'failure'. When the routine
       has finished its work, it is set to a value which means
       'success'. */
    int status = -1;
    /* The following number is set by trial and error only. I cannot
       see where it it is documented in the libpng manual.
    */
    int pixel_size = 3;
    int depth = 8;
    
    fp = fopen (path, "wb");
    if (! fp) {
        goto fopen_failed;
    }

    png_ptr = png_create_write_struct (PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    if (png_ptr == NULL) {
        goto png_create_write_struct_failed;
    }
    
    info_ptr = png_create_info_struct (png_ptr);
    if (info_ptr == NULL) {
        goto png_create_info_struct_failed;
    }
    
    /* Set up error handling. */

    if (setjmp (png_jmpbuf (png_ptr))) {
        goto png_failure;
    }
   
    crop_t cropx;
    if (crop == NULL) {
        cropx.top = 0;
        cropx.bottom = bitmap->height;
        cropx.left = 0;
        cropx.right = bitmap->width;
        crop = &cropx;
    }
 
    /* Set image attributes. */
    size_t width = crop->right - crop->left;
    size_t height = crop->bottom - crop->top;

    png_set_IHDR (png_ptr,
                  info_ptr,
		  width,
                  height,
                  depth,
                  PNG_COLOR_TYPE_RGB,
                  PNG_INTERLACE_NONE,
                  PNG_COMPRESSION_TYPE_DEFAULT,
                  PNG_FILTER_TYPE_DEFAULT);
    
    /* Initialize rows of PNG. */

    row_pointers = png_malloc (png_ptr, height * sizeof (png_byte *));
    for (y = crop->top; y < crop->bottom; ++y) {
        png_byte *row = 
            png_malloc (png_ptr, sizeof (uint8_t) * width * pixel_size);
        row_pointers[y - crop->top] = row;
        for (x = crop->left; x < crop->right; ++x) {
            //pixel_t * pixel = pixel_at (bitmap, x, y);
			pixel_t * pixel = yellow_at (bitmap, x, y);
            *row++ = pixel->red;
            *row++ = pixel->green;
            *row++ = pixel->blue;
        }
    }
    
    /* Write the image data to "fp". */

    png_init_io (png_ptr, fp);
    png_set_rows (png_ptr, info_ptr, row_pointers);
    png_write_png (png_ptr, info_ptr, PNG_TRANSFORM_IDENTITY, NULL);

    /* The routine has successfully written the file, so we set
       "status" to a value which indicates success. */

    status = 0;
    
    for (y = 0; y < height; y++) {
        png_free (png_ptr, row_pointers[y]);
    }
    png_free (png_ptr, row_pointers);
    
 png_failure:
 png_create_info_struct_failed:
    png_destroy_write_struct (&png_ptr, &info_ptr);
 png_create_write_struct_failed:
    fclose (fp);
 fopen_failed:
    return status;
}
