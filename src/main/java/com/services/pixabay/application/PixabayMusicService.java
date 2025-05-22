package com.services.pixabay.application;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.services.common.application.DataInitializationService;
import com.services.common.application.dto.CustomResponse;
import com.services.common.domain.DataStorage;
import com.services.pixabay.application.dto.PixabayMusicRequest;
import com.services.pixabay.domain.PixabayMusic;

@Service
public class PixabayMusicService extends DataInitializationService {
    
    private static final String PIXABAY_MUSIC_URL = "https://api.4d4cat.site/pixabay/music/search/filter";
    private static final String[] PIXABAY_MUSIC_GENRES = {
        "electronic","upbeat","beats","beautiful%20plays","main%20title","alternative%20hip%20hop","modern%20classical","ambient","build%20up%20scenes","acoustic%20group","solo%20piano","corporate","solo%20instruments","rnb","action","intro%2Foutro","rock","folk","adventure","vocal","mystery","chase%20scene","indie%20pop","pulses","meditation%2Fspiritual","small%20emotions","alternative","nostalgia","trap","high%20drones","mainstream%20hip%20hop","solo%20classical%20instruments",
        /* "soft%20house","epic%20classical","techno%20%26%20trance","pop","house","classical%20piano","happy%20childrens%20tunes","suspense","cafe","future%20bass","synthwave","traditional%20jazz","video%20games","solo%20guitar","hard%20rock","world","dance","electro","horror%20scene","supernatural","high%20rhythmic%20drones","special%20occasions","christmas","crime%20scene","cartoons","eccentric%20%26%20quirky","small%20drama","elevator%20music","funk","drama%20scene","jingles","vintage",
        "low%20drones","synth%20pop","old%20school%20hip%20hop","marching%20band","metal","modern%20country","lullabies","fantasy%20%26%20dreamy%20childrens","deep%20house","smooth%20jazz","chamber%20music","dramatic%20classical","drum%20n%20bass","sneaky","modern%20jazz","bloopers","island","afrobeat","religious%20theme","choir","acid%20jazz","dubstep","comedy","motown%20%26%20old%20school%20rnb","blues","modern%20blues","ireland","strange%20%26%20weird","scotland","wedding","post%20rock","amusement%20park",
        "scary%20childrens%20tunes","gospel","reggae","traditional%20country","bossa%20nova","china","low%20rhythmic%20drones","big%20band","urban%20latin","funerals","old%20school%20funk","vaudeville%20%26%20variety%20show","show%20dance","tragedy","high%20non%20rhythmic%20drones","low%20non%20rhythmic%20drones","ska","old%20school%20rnb","india","samba%20%28latin%29","fantasy%20%26%20dreamy%20childrens%27","military%20%26%20historical","punk","classical%20string%20quartet","cha%20cha%20%28latin%29","circus","usa","american%20roots%20rock","oompah%20band","tango","polka","france","greece" */
    };
    
    public PixabayMusicService(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    protected String getBaseUrl() {
        return PIXABAY_MUSIC_URL;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeMusicData() {
        List<PixabayMusic> musicList = new ArrayList<>();
        for (String genre : PIXABAY_MUSIC_GENRES) {
            try {
                PixabayMusicRequest request = new PixabayMusicRequest(genre);
                CustomResponse<PixabayMusic> response = get(new ParameterizedTypeReference<CustomResponse>() {}, request);
                if (response != null && response.results() != null) {
                    musicList.addAll(response.results());
                }
            } catch (Exception e) {
                System.err.println("Error fetching data from Pixabay: " + e.getMessage());
                e.printStackTrace();
            }
        }
        DataStorage.setData("pixabayMusic", musicList);
    }
}
