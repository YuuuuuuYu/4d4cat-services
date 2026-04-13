package com.services.api.techblog;

import com.services.api.techblog.dto.TechBlogListResponse;
import com.services.core.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/techblogs")
@RequiredArgsConstructor
public class TechBlogController {

  private final TechBlogQueryService queryService;

  @GetMapping
  public ResponseEntity<BaseResponse<TechBlogListResponse>> getTechBlogs(
      @RequestParam(required = false) Long cursorId,
      @RequestParam(required = false) List<String> companySlug,
      @RequestParam(required = false) String tag) {

    TechBlogListResponse data = queryService.getTechBlogs(cursorId, companySlug, tag);
    return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK, data));
  }

  @GetMapping("/companies/slugs")
  public ResponseEntity<BaseResponse<List<String>>> getActiveCompanySlugs() {
    List<String> slugs = queryService.getActiveCompanySlugs();
    return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK, slugs));
  }

  @PostMapping("/{id}/click")
  public ResponseEntity<BaseResponse<Void>> incrementClickCount(@PathVariable Long id) {
    queryService.incrementClickCount(id);
    return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK, null));
  }
}
