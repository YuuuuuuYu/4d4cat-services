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
        "electronic"
        // ,"upbeat","beats","beautiful+plays","main+title","alternative+hip+hop","modern+classical","ambient","build+up+scenes","acoustic+group","solo+piano","corporate","solo+instruments","rnb","action","intro%2Foutro","rock","folk","adventure","vocal","mystery","chase+scene","indie+pop","pulses","meditation%2Fspiritual","small+emotions","alternative","nostalgia","trap","high+drones","mainstream+hip+hop","solo+classical+instruments",
        // "soft+house","epic+classical","techno+%26+trance","pop","house","classical+piano","happy+childrens+tunes","suspense","cafe","future+bass","synthwave","traditional+jazz","video+games","solo+guitar","hard+rock","world","dance","electro","horror+scene","supernatural","high+rhythmic+drones","special+occasions","christmas","crime+scene","cartoons","eccentric+%26+quirky","small+drama","elevator+music","funk","drama+scene","jingles","vintage",
        // "low+drones","synth+pop","old+school+hip+hop","marching+band","metal","modern+country","lullabies","fantasy+%26+dreamy+childrens","deep+house","smooth+jazz","chamber+music","dramatic+classical","drum+n+bass","sneaky","modern+jazz","bloopers","island","afrobeat","religious+theme","choir","acid+jazz","dubstep","comedy","motown+%26+old+school+rnb","blues","modern+blues","ireland","strange+%26+weird","scotland","wedding","post+rock","amusement+park",
        // "scary+childrens+tunes","gospel","reggae","traditional+country","bossa+nova","china","low+rhythmic+drones","big+band","urban+latin","funerals","old+school+funk","vaudeville+%26+variety+show","show+dance","tragedy","high+non+rhythmic+drones","low+non+rhythmic+drones","ska","old+school+rnb","india","samba+%28latin%29","fantasy+%26+dreamy+childrens%27","military+%26+historical","punk","classical+string+quartet","cha+cha+%28latin%29","circus","usa","american+roots+rock","oompah+band","tango","polka","france","greece"
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
