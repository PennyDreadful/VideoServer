package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Rating;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

	// TODO replace this with an autowired 	VideoRepository, since that
	// makes it easy to swap implementations.
	private Map<Long, Video> videoMap = new ConcurrentHashMap<>();
	private Map<Long, Rating> ratingMap = new ConcurrentHashMap<>();
	
    private static final AtomicLong currentId = new AtomicLong(0L);


  	private Video save(Video entity) {
		checkAndSetId(entity);
		videoMap.put(entity.getId(), entity);
		ratingMap.put(entity.getId(), new Rating());
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videoMap.values();
	}


	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v) {	
		// TODO Deal with null videos.
		save(v);
		String dataUrl = getDataUrl(v.getId());
		v.setDataUrl(dataUrl);

		return v;
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_STAR_PATH, method=RequestMethod.POST)
	public @ResponseBody Double addRating(@PathVariable("id") long id, @PathVariable("stars") long stars) {	
		Rating rating = ratingMap.get(id);
		if(stars > 5){
			stars = 5;
		} else if(stars < 1){
			stars = 1;
		}
		rating.updateRating(stars);

		return rating.getAverage();
	}
	
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus uploadVideo(@PathVariable("id") long id,
			@RequestParam("data") MultipartFile videoData,
			HttpServletResponse response) throws IOException {
		Video v = videoMap.get(id);
		if (v == null){
			response.sendError(404);
		} else {
			try (InputStream in = videoData.getInputStream()){	
				VideoFileManager.get().saveVideoData(v, in);
			}
		}
		return new VideoStatus(VideoState.READY);
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.GET)
	public void getVideoData(@PathVariable("id") long id,
			HttpServletResponse response) throws IOException{
		Video video = videoMap.get(id);
		if (video == null){
			response.sendError(404);
		} else {
			System.out.println("Serving video" + video.getTitle());
			try(OutputStream out = response.getOutputStream()){
				response.setContentType(video.getContentType());
				VideoFileManager.get().copyVideoData(video, out);
			}
		}
	}


	 private String getDataUrl(long videoId){
         String url = getUrlBaseForLocalServer() + VideoSvcApi.VIDEO_SVC_PATH + "/" + videoId +
        		  "/" + VideoSvcApi.DATA_PARAMETER;
         return url;
     }

  	private String getUrlBaseForLocalServer() {
		   HttpServletRequest request = 
		       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		   String base = 
		      "http://"+request.getServerName() 
		      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
		   return base;
		}
}
