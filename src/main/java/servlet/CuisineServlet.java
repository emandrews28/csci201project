package servlet;

import com.google.gson.Gson;
import dao.CuisineDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * GET /api/cuisines
 * Returns the full list of cuisines to populate the filter dropdown.
 * No auth required — this is public data.
 */
@WebServlet("/api/cuisines")
public class CuisineServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final CuisineDAO cuisineDAO = new CuisineDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(cuisineDAO.findAll()));
    }
}
