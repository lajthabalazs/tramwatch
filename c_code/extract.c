#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <unistd.h>
#include <inttypes.h>
#include <vlc/vlc.h>

#include "pnghelper.h"
#include "trigger.h"

int done = 0;
libvlc_media_player_t *media_player = NULL;
uint8_t *videoBuffer = 0;
unsigned int videoBufferSize = 0;

int framenum = 0;
int skip = 0;
int frames = 0;

bitmap_t bitmap;
crop_t crop;

void cbVideoPrerender(void *p_video_data, uint8_t **pp_pixel_buffer, int size) {

       // Locking
    //printf("cbVideoPrerender %i\n",size);
    //printf("vtest: %lld\n",(long long int)p_video_data);
    if (size > videoBufferSize || !videoBuffer)
    {
        printf("Reallocate raw video buffer\n");
        if(videoBuffer) free(videoBuffer);
        videoBuffer = (uint8_t *)malloc(size);
        videoBufferSize = size;
    }
    *pp_pixel_buffer = videoBuffer;
    bitmap.pixels = (pixel_t*) videoBuffer;
}

void cbVideoPostrender(void *p_video_data, uint8_t *p_pixel_buffer,
      int width, int height, int pixel_pitch, int size, int64_t pts) {

    frames++;

    if (frames % skip != 0) return;

    printf("cbVideoPostrender %i\n",size);
    printf("frame: %d, w x h: %d x %d, pitch: %d\n",
      framenum, width, height, pixel_pitch);
    char filename[128];
    sprintf(filename, "frame%05d.png", framenum++);
    bitmap.width = width;
    bitmap.height = height;
	int f = store_frame(&bitmap, &crop);
	
	write_frame(f, filename);
	
	calculate_alldiffs(f);
	
    //save_png_to_file_crop(&bitmap, &crop, filename);
    //save_png_to_file(&bitmap, filename);
   // Unlocking
}

int main(int argc, char **argv)
{

	crop.top = 120;
	crop.bottom = 176;
	crop.left = 200;
	crop.right = 288;

	skip = 30;

        // VLC pointers
        libvlc_instance_t *vlcInstance;
        void *pUserData = 0;

        // VLC options
       char smem_options[1000];
       sprintf(smem_options
          , "#transcode{vcodec=RV24}:smem{"
             "video-prerender-callback=%lld,"
             "video-postrender-callback=%lld,"
             "video-data=%lld},"
          , (long long int)(intptr_t)(void*)&cbVideoPrerender
          , (long long int)(intptr_t)(void*)&cbVideoPostrender
          , (long long int)200); //Test data

        const char * const vlc_args[] = {
              "-I", "dummy", // Don't use any interface
              "--ignore-config", // Don't use VLC's config
              "--extraintf=logger", // Log anything
              "--verbose=1", // Be verbose
              "--no-sout-audio",
              "--sout", smem_options // Stream to memory
               };

		fdbuffer_init();
			   
        // We launch VLC
        vlcInstance = libvlc_new(sizeof(vlc_args) / sizeof(vlc_args[0]), vlc_args);

        media_player = libvlc_media_player_new(vlcInstance);

        libvlc_media_t *media = libvlc_media_new_path(vlcInstance, "http://www.utv.hu/swfs/103.swf");
        libvlc_media_player_set_media(media_player, media);
        libvlc_media_player_play(media_player);
        
        while (!done)
        {
            sleep(1);         
            //libvlc_media_player_set_position(media_player, 0.);
        }
        libvlc_media_release(media);
		fdbuffer_destroy();
		
        return 0;
}

