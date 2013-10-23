package gov.nysenate.billbuzz.util;

import gov.nysenate.billbuzz.model.BillBuzzConfirmation;
import gov.nysenate.billbuzz.model.BillBuzzSubscription;
import gov.nysenate.billbuzz.model.BillBuzzUser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

public class FormProcessor
{
    public static BillBuzzConfirmation getConfirmation(HttpServletRequest request) throws SQLException
    {
        String code = request.getParameter("key");
        if (code != null) {
            BillBuzzDAO dao = new BillBuzzDAO();
            BillBuzzConfirmation confirmation = dao.loadConfirmation("update", code);
            if (confirmation != null && !confirmation.isExpired()) {
                return confirmation;
            }
        }
        return null;
    }

    public static Map<String, TreeSet<String>> getSubscriptionMap(List<BillBuzzSubscription> subscriptions)
    {
        Map<String, TreeSet<String>> subscriptionMap = new HashMap<String, TreeSet<String>>();
        subscriptionMap.put("sponsor", new TreeSet<String>());
        subscriptionMap.put("party", new TreeSet<String>());
        for (BillBuzzSubscription subscription : subscriptions) {
            subscriptionMap.get(subscription.getCategory()).add(subscription.getValue());
        }
        return subscriptionMap;
    }

    public static BillBuzzUser processRequestForm(HttpServletRequest request) throws SQLException
    {
        BillBuzzDAO dao = new BillBuzzDAO();
        String email = request.getParameter("email");
        if (email == null) {
            return null;
        }
        else {
            return dao.loadUser(email);
        }
    }

    /**
    *
    * @param request
    * @param user
    * @param createdAt
    * @return A saved user with its new subscriptions if the form if valid. null if the form is invalid.
    * @throws SQLException
    */
   public static BillBuzzUser processSubscriptionForm(HttpServletRequest request, Date createdAt) throws SQLException
   {
       BillBuzzDAO dao = new BillBuzzDAO();

       String email = request.getParameter("email");
       String email2 = request.getParameter("email2");
       String firstName = request.getParameter("firstName");
       String lastName = request.getParameter("lastName");

       if (email == null || email.isEmpty() || firstName == null || firstName.isEmpty() || lastName == null || lastName.isEmpty() || email2 == null || email2.isEmpty() || !email.equals(email2)) {
           return null;
       }

       BillBuzzUser user = dao.loadUser(email);
       if (user == null) {
           user = new BillBuzzUser(email, firstName, lastName, createdAt);
           dao.saveUser(user);
       }
       else {
           user.setFirstName(firstName);
           user.setLastName(lastName);
       }

       String all = request.getParameter("all");
       String[] senators = request.getParameterValues("senators");
       String[] parties = request.getParameterValues("parties");
       List<BillBuzzSubscription> subscriptions = new ArrayList<BillBuzzSubscription>();
       if (all != null) {
           BillBuzzSubscription subscription = new BillBuzzSubscription(user.getId(), "sponsor", "all", createdAt);
           subscriptions.add(subscription);
       }
       else {
           if (senators != null) {
               for (String sponsor : senators) {
                   BillBuzzSubscription subscription = new BillBuzzSubscription(user.getId(), "sponsor", sponsor, createdAt);
                   subscriptions.add(subscription);
               }
           }

           if (parties != null) {
               for (String party : parties) {
                   BillBuzzSubscription subscription = new BillBuzzSubscription(user.getId(), "party", party, createdAt);
                   subscriptions.add(subscription);
               }
           }
       }
       user.setSubscriptions(subscriptions);

       return user;
   }

   public static List<BillBuzzSubscription> getSubscriptions(HttpServletRequest request) {
       String all = request.getParameter("all");
       String[] senators = request.getParameterValues("senators");
       String[] parties = request.getParameterValues("parties");
       List<BillBuzzSubscription> subscriptions = new ArrayList<BillBuzzSubscription>();
       if (all != null) {
           BillBuzzSubscription subscription = new BillBuzzSubscription(0, "sponsor", "all", new Date());
           subscriptions.add(subscription);
       }
       else {
           if (senators != null) {
               for (String sponsor : senators) {
                   BillBuzzSubscription subscription = new BillBuzzSubscription(0, "sponsor", sponsor, new Date());
                   subscriptions.add(subscription);
               }
           }

           if (parties != null) {
               for (String party : parties) {
                   BillBuzzSubscription subscription = new BillBuzzSubscription(0, "party", party, new Date());
                   subscriptions.add(subscription);
               }
           }
       }
       return subscriptions;
   }
}