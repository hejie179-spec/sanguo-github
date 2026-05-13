package com.sanguo.controller;

import com.sanguo.common.BizException;
import com.sanguo.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Api(tags = "文件")
@RestController
@RequestMapping("/file")
public class FileController {

    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp"));
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.public-path:/uploads}")
    private String publicPath;

    @ApiOperation("上传图片")
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadImage(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择图片文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException("图片大小不能超过 10MB");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BizException("仅支持图片类型文件");
        }

        String original = file.getOriginalFilename();
        String ext = getExt(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("仅支持 jpg/jpeg/png/gif/webp 格式图片");
        }

        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String relative = "images/" + day + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dest = root.resolve(relative).normalize();
        if (!dest.startsWith(root)) {
            throw new BizException("非法文件路径");
        }
        try {
            Files.createDirectories(dest.getParent());
            file.transferTo(dest.toFile());
        } catch (IOException e) {
            throw new BizException(500, "上传失败");
        }

        String ctx = request.getContextPath();
        String base = publicPath.startsWith("/") ? publicPath : "/" + publicPath;
        String url = ctx + (base.endsWith("/") ? base : base + "/") + relative;

        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("path", relative);
        return Result.ok(data);
    }

    private String getExt(String filename) {
        if (!StringUtils.hasText(filename)) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
