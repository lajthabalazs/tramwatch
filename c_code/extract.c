#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <unistd.h>
#include <inttypes.h>
#include <errno.h>
#include <string.h>
#include <sys/stat.h>
#include <vlc/vlc.h>
#include <jansson.h>

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

typedef struct {
 char name[32];
 int thMin;
 int thMax;
 int div;
 triggerchain_t *triggerChain;
} triggers_t;

triggers_t triggers[64];
int triggerCount = 0;

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
    bitmap.width = width;
    bitmap.height = height;
	int f = store_frame(&bitmap, &crop);
		
	calculate_alldiffs(f);
	int t;
	for (t = 0; t < triggerCount; t++) {
		int r = sumtrigger(triggers[t].triggerChain);
		if (r > triggers[t].thMin && r < triggers[t].thMax) {
			printf("Trigger! %s[%d] (%d)\n", triggers[t].name, t, r);
			sprintf(filename, "frame%05d.png", framenum);
			write_frame(f, filename);
			sprintf(filename, "frame%05d.png", framenum-1);
			write_frame(f-1, filename);
			sprintf(filename, "frame%05d.png", framenum-2);
			write_frame(f-2, filename);
			sprintf(filename, "frame%05d.png", framenum-3);
			write_frame(f-3, filename);
			sprintf(filename, "frame%05d.png", framenum-4);
			write_frame(f-4, filename);
		}
		else /*if (r != 0)*/ {
			printf("DEBUG: No trigger %s[%d] (%d)\n", triggers[t].name, t, r);
		}
	}

    framenum++;	
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
	
	char *triggerfile = NULL;
	
	int c;
	while ((c = getopt (argc, argv, "t:")) != -1)
        switch (c) {
			case 't':
            triggerfile = optarg;
            break;

			default:
            abort ();
        }

	if (triggerfile) {
    
		json_error_t error;

		json_t *json = json_load_file(triggerfile, 0, &error);
		if (!json) {
			printf("Unable to load parameters from %s! error: on line %d: %s\n", 
				triggerfile, error.line, error.text);
				exit -1;
		}
		
		json_t *tjson = json_object_get(json, "triggers");
		if (!tjson) {
			printf("Unable to get triggers from file: %s!\n", triggerfile);
			exit -1;
		}
				
		int i;
		for (i = 0; i < json_array_size(tjson); i++) {
			json_t *trigger = json_array_get(tjson, i);
			json_t *trName = json_object_get(trigger, "name");
			json_t *trMinT = json_object_get(trigger, "minThreshold");
			json_t *trMaxT = json_object_get(trigger, "maxThreshold");
			json_t *trCoeff = json_object_get(trigger, "coefficients");
			
			strncpy((char*) &triggers[triggerCount].name, json_string_value(trName), 31);
			triggers[triggerCount].thMin = json_integer_value(trMinT);
			triggers[triggerCount].thMax = json_integer_value(trMaxT);
			triggers[triggerCount].div = 0;
			triggers[triggerCount].triggerChain = NULL;
			
			int j;
			for (j = 0; j < json_array_size(trCoeff); j++) {
				json_t *coeff = json_array_get(trCoeff, j);
				json_t *trcImage = json_object_get(coeff, "image");
				json_t *trcX = json_object_get(coeff, "x");
				json_t *trcY = json_object_get(coeff, "y");
				json_t *trcC = json_object_get(coeff, "c");
				
				triggerchain_t *tc = (triggerchain_t*) malloc(sizeof(triggerchain_t));
				tc->frame = json_integer_value(trcImage) - 1;
				tc->x = json_integer_value(trcX);
				tc->y = json_integer_value(trcY);
				tc->c = json_integer_value(trcC);
				if (tc->c > 0) triggers[triggerCount].div++;
				
				triggerchain_t *ttc = triggers[triggerCount].triggerChain;
				triggers[triggerCount].triggerChain = tc;
				tc->next = ttc;
			}
			
			printf("Trigger[%d] name: %s, %d coefficients\n", triggerCount, json_string_value(trName), j);
			triggerCount++;
		}
	}
		
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

