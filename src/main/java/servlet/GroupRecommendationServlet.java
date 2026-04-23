package servlet;

import com.google.gson.Gson;
import model.RecommendationGroup;
import model.Restaurant;
import dao.GroupRecommendationDAO;
import service.GroupRecommendationManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/groups")
public class GroupRecommendationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final GroupRecommendationDAO dao = new GroupRecommendationDAO();
    private final GroupRecommendationManager manager = new GroupRecommendationManager();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // create group
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }

        long userId = (long) session.getAttribute("userId");
        RecommendationGroup g = gson.fromJson(request.getReader(), RecommendationGroup.class);
        if (g.getGroupName() == null) g.setGroupName("");
        g.setCreatedBy(userId);
        RecommendationGroup created = dao.createGroup(g);
        if (created != null) {
            // add creator as member
            dao.addGroupMember(created.getGroupId(), userId);
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(gson.toJson(created));
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Failed to create group\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // add/remove member via action param
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");
        String groupIdStr = request.getParameter("groupId");
        String userIdStr = request.getParameter("userId");

        if (action == null || groupIdStr == null || userIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"action, groupId, userId required\"}");
            return;
        }

        long groupId = Long.parseLong(groupIdStr);
        long userId = Long.parseLong(userIdStr);

        boolean ok = false;
        if (action.equals("add")) ok = dao.addGroupMember(groupId, userId);
        else if (action.equals("remove")) ok = dao.removeGroupMember(groupId, userId);
        else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"unknown action\"}");
            return;
        }

        if (ok) out.print("{\"message\":\"success\"}");
        else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"operation failed\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // get top three recommendations
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String groupIdStr = request.getParameter("groupId");
        if (groupIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"groupId required\"}");
            return;
        }
        long groupId = Long.parseLong(groupIdStr);
        List<Restaurant> top = manager.getTopThree(groupId);
        out.print(gson.toJson(top));
    }
}
