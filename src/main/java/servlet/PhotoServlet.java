package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.PhotoDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import model.Photo;
import util.PhotoStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.UUID;

@WebServlet("/api/photos")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5L * 1024 * 1024,
        maxRequestSize = 6L * 1024 * 1024
)
public class PhotoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final PhotoDAO photoDAO = new PhotoDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String restaurantIdStr = request.getParameter("restaurantId");
        String userIdStr = request.getParameter("userId");
        String reviewIdStr = request.getParameter("reviewId");

        try {
            List<Photo> photos;
            if (restaurantIdStr != null) {
                photos = photoDAO.findByRestaurant(Long.parseLong(restaurantIdStr));
            } else if (userIdStr != null) {
                photos = photoDAO.findByUser(Long.parseLong(userIdStr));
            } else if (reviewIdStr != null) {
                photos = photoDAO.findByReview(Long.parseLong(reviewIdStr));
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"restaurantId, userId, or reviewId required\"}");
                return;
            }
            out.print(gson.toJson(photos));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"invalid id parameter\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }
        long userId = (long) session.getAttribute("userId");

        String restaurantIdStr = request.getParameter("restaurantId");
        if (restaurantIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"restaurantId required\"}");
            return;
        }

        long restaurantId;
        try {
            restaurantId = Long.parseLong(restaurantIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"invalid restaurantId\"}");
            return;
        }

        if (!photoDAO.restaurantExists(restaurantId)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Restaurant not found\"}");
            return;
        }

        Long reviewId = null;
        String reviewIdStr = request.getParameter("reviewId");
        if (reviewIdStr != null && !reviewIdStr.isBlank()) {
            try {
                reviewId = Long.parseLong(reviewIdStr);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"invalid reviewId\"}");
                return;
            }
        }

        String caption = request.getParameter("caption");

        Part filePart;
        try {
            filePart = request.getPart("image");
        } catch (IllegalStateException e) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            out.print("{\"error\":\"File exceeds 5 MB limit\"}");
            return;
        }

        if (filePart == null || filePart.getSize() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"image file required\"}");
            return;
        }

        if (filePart.getSize() > PhotoStorage.MAX_FILE_SIZE_BYTES) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            out.print("{\"error\":\"File exceeds 5 MB limit\"}");
            return;
        }

        String submittedName = filePart.getSubmittedFileName();
        String contentType = filePart.getContentType();
        boolean validType = PhotoStorage.isAllowedContentType(contentType)
                || PhotoStorage.isAllowedExtension(submittedName);
        if (!validType) {
            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            out.print("{\"error\":\"Only JPEG, PNG, or HEIC images are accepted\"}");
            return;
        }

        String ext = PhotoStorage.getExtension(submittedName);
        if (ext == null) ext = PhotoStorage.extensionForContentType(contentType);

        String filename = UUID.randomUUID() + "." + ext;
        String relativePath;
        try (InputStream in = filePart.getInputStream()) {
            relativePath = PhotoStorage.saveFile(userId, restaurantId, filename, in);
        } catch (IOException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Failed to save file\"}");
            return;
        }

        String imageUrl = request.getContextPath() + "/photos/files/" + relativePath;

        Photo photo = new Photo(userId, restaurantId, reviewId, imageUrl, caption);
        Photo created = photoDAO.createPhoto(photo);

        if (created == null) {
            PhotoStorage.deleteFile(relativePath);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Failed to save photo metadata\"}");
            return;
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        out.print(gson.toJson(created));
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }
        long userId = (long) session.getAttribute("userId");

        JsonObject body;
        try {
            body = gson.fromJson(request.getReader(), JsonObject.class);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"invalid JSON body\"}");
            return;
        }

        if (body == null || !body.has("photoId")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"photoId required\"}");
            return;
        }

        long photoId = body.get("photoId").getAsLong();
        String caption = body.has("caption") && !body.get("caption").isJsonNull()
                ? body.get("caption").getAsString()
                : null;

        boolean updated = photoDAO.updateCaption(photoId, userId, caption);
        if (updated) {
            out.print("{\"message\":\"Caption updated\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Photo not found or not owned by user\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }
        long userId = (long) session.getAttribute("userId");

        String photoIdStr = request.getParameter("photoId");
        if (photoIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"photoId required\"}");
            return;
        }

        long photoId;
        try {
            photoId = Long.parseLong(photoIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"invalid photoId\"}");
            return;
        }

        Photo existing = photoDAO.findById(photoId);
        if (existing == null || existing.getUserId() != userId) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Photo not found\"}");
            return;
        }

        boolean deleted = photoDAO.deletePhoto(photoId, userId);
        if (!deleted) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Failed to delete photo\"}");
            return;
        }

        String imageUrl = existing.getImageUrl();
        String prefix = request.getContextPath() + "/photos/files/";
        if (imageUrl != null && imageUrl.startsWith(prefix)) {
            PhotoStorage.deleteFile(imageUrl.substring(prefix.length()));
        }

        out.print("{\"message\":\"Photo deleted\"}");
    }
}
