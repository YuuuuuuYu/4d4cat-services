package com.services.api.pixabay;

import com.services.core.dto.BaseResponse;
import com.services.core.pixabay.dto.PixabayMusicResult;
import com.services.core.pixabay.dto.PixabayVideoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PixabayController {

  private final PixabayService pixabayService;

  @GetMapping("")
  public ResponseEntity<BaseResponse<String>> index() {
    return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK, "online"));
  }

  @GetMapping("/video")
  public ResponseEntity<BaseResponse<PixabayVideoResult>> video() {
    PixabayVideoResult data = pixabayService.getRandomVideo();
    return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK, data));
  }

  @GetMapping("/music")
  public ResponseEntity<BaseResponse<PixabayMusicResult>> music() {
    PixabayMusicResult data = pixabayService.getRandomMusic();
    return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK, data));
  }
}
