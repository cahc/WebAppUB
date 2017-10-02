package cc.org.w00t;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Cristian on 2017-09-29.
 */
@WebServlet(name = "PlayServlet", urlPatterns = {"/upload","/sendBack","/mapSize"},

        initParams =  {@WebInitParam(name = "Admin",value="Apan Ola"),
                @WebInitParam(name = "Admin2",value="Apan Ola2")

        }

)


public class hello extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.getWriter().append("Testing doPost");

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.getWriter().append("Testing doGet");

    }
}
