package servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.PhotoStorage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@WebServlet("/photos/files/*")
public class PhotoFileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String relative = pathInfo.substring(1);
        Path target = PhotoStorage.resolveSafe(relative);
        if (target == null || !Files.exists(target) || !Files.isRegularFile(target)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = contentTypeForFile(target.getFileName().toString());
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "public, max-age=3600");
        response.setContentLengthLong(Files.size(target));

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(target, out);
        }
    }

    private String contentTypeForFile(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".heic") || lower.endsWith(".heif")) return "image/heic";
        return "image/jpeg";
    }
}
