package gov.nysenate.billbuzz.controller;

import gov.nysenate.billbuzz.model.BillBuzzConfirmation;
import gov.nysenate.billbuzz.model.BillBuzzSubscription;
import gov.nysenate.billbuzz.model.BillBuzzUser;
import gov.nysenate.billbuzz.util.Application;
import gov.nysenate.billbuzz.util.BillBuzzDAO;
import gov.nysenate.billbuzz.util.FormProcessor;
import gov.nysenate.billbuzz.util.Mailer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.mail.EmailException;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

/**
 * Handles BillBuzz/(signup/(form)?)? requests.
 *
 * SignUp requests are posted to the server while form loads and confirmations
 * use get requests.
 *
 * @author GraylinKim
 *
 */
@SuppressWarnings("serial")
public class SignupForm extends HttpServlet
{
    private final Logger logger = Logger.getLogger(SignupForm.class);

    /**
     * Render the signup form.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            BillBuzzDAO dao = new BillBuzzDAO();
            request.setAttribute("message", "instruction");
            request.setAttribute("senators", dao.getSenators(dao.getSession()));
            request.setAttribute("subscriptions", FormProcessor.getSubscriptionMap(new ArrayList<BillBuzzSubscription>()));
            request.getRequestDispatcher("/WEB-INF/pages/signup_form.jsp").forward(request, response);
        }
        catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }
    }

    /**
     * Process the sign-up form and send confirmation email
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try {
            String message = "";
            Date now = new Date();
            BillBuzzDAO dao = new BillBuzzDAO();
            BillBuzzUser user = FormProcessor.processSubscriptionForm(request, now);
            List<BillBuzzSubscription> subscriptions = new ArrayList<BillBuzzSubscription>();
            if (user == null) {
                message = "missing_userinfo";
                subscriptions = FormProcessor.getSubscriptions(request, 0L, now);
            }
            else if (user.getSubscriptions().isEmpty()) {
                message = "missing_subscription";
            }
            else if (user.getConfirmedAt() == null) {
                logger.info("Processing signup for: "+user.getFirstName()+" <"+user.getEmail()+">");
                // Save subscriptions
                subscriptions = user.getSubscriptions();
                dao.replaceSubscriptions(subscriptions, user.getId());

                QueryRunner runner = new QueryRunner(Application.getDB().getDataSource());
                BillBuzzConfirmation confirmation = runner.query("SELECT * FROM billbuzz_confirmation WHERE userId = ? AND action = 'signup' AND usedAt=NULL", new BeanHandler<BillBuzzConfirmation>(BillBuzzConfirmation.class), user.getId());
                if (confirmation == null) {
                    logger.info("Creating new signup confirmation for: "+user.getEmail());
                    confirmation = new BillBuzzConfirmation(user.getId(), "signup", UUID.randomUUID().toString(), now, null);
                    dao.saveConfirmation(confirmation);
                }

                VelocityContext context = new VelocityContext();
                context.put("user", user);
                context.put("request", request);
                context.put("confirmation", confirmation);
                Mailer.send("signup_confirmation", "BillBuzz Signup Confirmation", user, context);

                message = "success";
            }
            else {
                // This user should be updating their account not signing up, send them update code
                BillBuzzConfirmation confirmation = new BillBuzzConfirmation(user.getId(), "update", UUID.randomUUID().toString(), now, null);
                dao.saveConfirmation(confirmation);

                // Send update email
                VelocityContext context = new VelocityContext();
                context.put("user", user);
                context.put("request", request);
                context.put("confirmation", confirmation);
                Mailer.send("update_confirmation", "BillBuzz Update Request", user, context);

                message = "update_required";
            }

            request.setAttribute("user", user);
            request.setAttribute("message", message);
            request.setAttribute("subscriptions", FormProcessor.getSubscriptionMap(subscriptions));
            request.setAttribute("senators", dao.getSenators(dao.getSession()));
            request.getRequestDispatcher("/WEB-INF/pages/signup_form.jsp").forward(request, response);
        }
        catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }
        catch (EmailException e) {
            logger.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }
        catch (MessagingException e) {
            logger.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }
    }
}
