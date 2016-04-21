package org.phiresearchlab.jira.servlet;


/**
 * Created by trunguyen on 4/9/2016.
 */


import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.user.util.UserManager;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//import com.atlassian.jira.rest.client.JiraRestClientFactory;
//import com.atlassian.jira.rest.client.domain.Field;
//import com.atlassian.jira.rest.client.domain.Issue;
//import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
//import com.atlassian.jira.rest.client.api.JiraRestClient;


public class ExportDateServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ExportDateServlet.class);
    private static final String JIRA_URL = "http://localhost:8091/";
    private static final String JIRA_URL2 = "http://localhost:2990/jira/";
    private static final String JIRA_ADMIN_USERNAME = "admin";
    private static final String JIRA_ADMIN_PASSWORD = "4rfvVGY&";
    private static final String JIRA_ADMIN_PASSWORD2 = "admin";
    private final IssueManager issueManager;
    private UserManager userManager;
    private CustomFieldManager customFieldManager;
    private static final int reserveDateCount = 5;



    public ExportDateServlet(IssueManager issueManager, UserManager userManager, CustomFieldManager customFieldManager) {
        this.issueManager = issueManager;
        this.userManager = userManager;
        this.customFieldManager = customFieldManager;

    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {   String baseUrl, issueUrl = null;
        MutableIssue currentIssue=null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        String issueKey = req.getParameter("issuekey");
        String fileName = "\""+ issueKey + ".ics\"";
        res.setContentType("text/calendar");
        res.setHeader("Content-Disposition", "attachment; filename="+fileName);
        OutputStream outputStream = res.getOutputStream();
        //MutableIssue currentIssue = getIssue(issueKey);
        try {
            // retrieve  issue data
            currentIssue = issueManager.getIssueObject(issueKey);
        }
        catch (Exception e) {
            outputStream.write(e.toString().getBytes());
        }
        try
        {
            baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
            issueUrl = baseUrl + "/browse/" + issueKey;
            Calendar cal = new Calendar();
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            VEvent roomReserveEvent = null;
            Date sdata = null;
            Date edata = null;
            DateTime startTime = null;
            DateTime endTime = null;
            CustomField reserveDate;
            String summary = currentIssue.getSummary();
            String idxStr="";
            cal.getProperties()
                    .add(new ProdId(
                            "-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN"));
            cal.getProperties().add(Version.VERSION_2_0);
            cal.getProperties().add(CalScale.GREGORIAN);

            // add the event to calendar
            try {
                if (customFieldManager.getCustomFieldObjectByName("Start Date and Time") != null) {
                    reserveDate = customFieldManager.getCustomFieldObjectByName("Start Date and Time");
                    try {
                        sdata = (Timestamp) reserveDate.getValue(currentIssue);
                        startTime = new DateTime(sdata.getTime());
                    }
                    catch (Exception e)
                    {
                        log.debug("Sdata was null");
                    }
                }
                if (customFieldManager.getCustomFieldObjectByName("End Date and Time") != null) {
                    reserveDate = customFieldManager.getCustomFieldObjectByName("End Date and Time");
                    try {
                        edata = (Timestamp) reserveDate.getValue(currentIssue);
                        endTime = new DateTime(edata.getTime());
                    }
                    catch (Exception e) {
                        log.debug("Edata was null");
                    }
                }
                roomReserveEvent = createEvent(startTime,endTime,issueKey,summary);
                if (roomReserveEvent != null) {
                    String uidval = currentIssue.getKey() + "@IIU";
                    Uid uid = new Uid(uidval);
                    roomReserveEvent.getProperties().add(uid);
                    // add location
                    roomReserveEvent.getProperties().add(new Location("Century Center Building 2500 Room:2029"));
                    // add description
                    roomReserveEvent.getProperties().add(new Description(currentIssue.getDescription()));

                    // add url
                    roomReserveEvent.getProperties().add(new Url(new URI(issueUrl)));
                    // add attendees..
                    Attendee att1 = new Attendee(URI.create(currentIssue.getReporter().getEmailAddress()));
                    //Attendee att1 = new Attendee(new URI("mailto",currentIssue.getReporter().getEmailAddress(),null));
                    att1.getParameters().add(Role.REQ_PARTICIPANT);
                    att1.getParameters().add(new Cn("Main POC: "+ currentIssue.getReporter().getDisplayName() ));
                    try {
                        roomReserveEvent.getProperties().add(att1);
                    } catch (Exception e) {
                        //outputStream.write(e.toString().getBytes());
                    }
//                    // add a default attendee
//                    Attendee att2 = new Attendee(URI.create(currentIssue.getReporter().getEmailAddress()));
//                    //Attendee att2 = new Attendee(new URI("mailto",currentIssue.getReporter().getEmailAddress(),null));
//                    att2.getParameters().add(Role.REQ_PARTICIPANT);
//                    att2.getParameters().add(new Cn("Secondary POC: "+ currentIssue.getReporter().getDisplayName() ));
//                    try {
//                        roomReserveEvent.getProperties().add(att2);
//                    } catch (Exception e) {
//                        //outputStream.write(e.toString().getBytes());
//                    }
                    cal.getComponents().add(roomReserveEvent);
                }

            }
            catch (Exception e) {

            }
            //outputStream.write("checkpoint before loop\n".getBytes());
            for (int i = 0; i < reserveDateCount; i ++) {  // for each set of date, create calendar item
                    reserveDate = null;
                    startTime= null;
                    endTime=null;
                    try {
                        idxStr =  new Integer(i+1).toString();
                        if (customFieldManager.getCustomFieldObjectByName("Start date and time (day " + idxStr + ")") != null) {
                            reserveDate = customFieldManager.getCustomFieldObjectByName("Start date and time (day " + idxStr + ")");
                            try {
                                sdata = (Timestamp) reserveDate.getValue(currentIssue);
                                startTime = new DateTime(sdata.getTime());
                            }
                            catch(Exception e) {
                                log.debug("Sdata was null");
                                //outputStream.write("sdate ".concat(e.toString()).getBytes());
                            }
                        }
                        if (customFieldManager.getCustomFieldObjectByName("End date and time (day " + idxStr + ")") != null) {
                            reserveDate = customFieldManager.getCustomFieldObjectByName("End date and time (day " + idxStr + ")");
                            try {
                                edata = (Timestamp) reserveDate.getValue(currentIssue);
                                endTime = new DateTime(edata.getTime());
                            }
                            catch (Exception e) {
                                log.debug("Edata was null");
                                //outputStream.write("edate ".concat(e.toString().concat("\n").getBytes());
                            }
                        }
                        roomReserveEvent = createEvent(startTime,endTime,issueKey,summary);
                        if (roomReserveEvent != null) {
                            String uidval = currentIssue.getKey() + "-" + idxStr + "@IIU";
                            Uid uid = new Uid(uidval);
                            roomReserveEvent.getProperties().add(uid);
                            // add location
                            roomReserveEvent.getProperties().add(new Location("Century Center Building 2500 Room:2029"));
                            // add description
                            String issueDesc = "This is to confirm your lab reservation request.  You can review your request at:\n" + issueUrl;
                                if (currentIssue.getDescription() != null) {
                                    issueDesc = currentIssue.getDescription().concat("\n\n").concat(issueDesc);
                                }
                            try {
                                roomReserveEvent.getProperties().add(new Description(issueDesc));
                            }
                            catch (Exception e) {
                               // outputStream.write("descp ".concat(e.toString()).getBytes());
                            }

                            // add url
                            roomReserveEvent.getProperties().add(new Url(new URI(issueUrl)));
                            // add organizer
                            try {
                                Organizer organizer = new Organizer(URI.create("mailto:" + currentIssue.getCreator().getEmailAddress()));
                                organizer.getParameters().add(new Cn("Organizer POC: " + currentIssue.getCreator().getDisplayName()));
                                    roomReserveEvent.getProperties().add(organizer);
                                } catch (Exception e) {
                                    // do nothing
                                    log.debug("organizer ".concat(e.toString()));
                                    //outputStream.write("organizer ".concat(e.toString()).getBytes());
                            }

                            // add attendees..
                            Attendee att1 = new Attendee(URI.create(currentIssue.getReporter().getEmailAddress()));
                            //Attendee att1 = new Attendee(new URI("mailto",currentIssue.getReporter().getEmailAddress(),null));
                            att1.getParameters().add(Role.REQ_PARTICIPANT);
                            att1.getParameters().add(new Cn("Main POC: "+ currentIssue.getReporter().getDisplayName() ));
                            try {
                                roomReserveEvent.getProperties().add(att1);
                            } catch (Exception e) {
                                //outputStream.write(e.toString().getBytes());
                            }
                            // add a default attendee
                                //Attendee att2 = new Attendee(URI.create(currentIssue.getReporter().getEmailAddress()));
//                            //Attendee att2 = new Attendee(new URI("mailto",currentIssue.getReporter().getEmailAddress(),null));
//                            att2.getParameters().add(Role.REQ_PARTICIPANT);
//                            att2.getParameters().add(new Cn("Secondary POC: "+ currentIssue.getReporter().getDisplayName() ));
//                            try {
//                                roomReserveEvent.getProperties().add(att2);
//                            } catch (Exception e) {
//                                //outputStream.write(e.toString().getBytes());
//                            }
                            cal.getComponents().add(roomReserveEvent);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        log.debug(e.toString());
                        //outputStream.write("loop  ".concat(e.toString()).getBytes());

                    }


            }
            // compelted adding events
           CalendarOutputter out = new CalendarOutputter();
            out.setValidating(false);
            try {
                out.output(cal,bout);  //output calendar to byte stream
                outputStream.write(bout.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                outputStream.write(e.toString().getBytes());
            }
            catch (ValidationException e) {
                e.printStackTrace();
                outputStream.write(e.toString().getBytes());
            }
        }
        catch(Exception e)
        {
            outputStream.write(e.toString().getBytes());
            outputStream.close();
        }
        finally {
            outputStream.flush();
            outputStream.close();
        }
    }

    protected MutableIssue getIssue(String issueKey){

        MutableIssue currentIssue = null;
        try {
        MutableIssue issue = issueManager.getIssueObject(issueKey);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return currentIssue;
    }

//
//    protected List<CustomField> getDateObjects(MutableIssue currentIssue) throws Exception {
//        List<CustomField> fields = new ArrayList<CustomField>();
//        try {
//            List<CustomField>customFields = customFieldManager.getCustomFieldObjects(currentIssue);
//            if (customFields != null) {
//                for(CustomField field: customFields) {
//                    if (field.getName().toLowerCase().contains("date") && field.getValue(currentIssue) != null) {
//                        fields.add(field);
//                    }
//                }
//            }
//        }
//        catch (Exception e) {
//
//            e.printStackTrace();
//
//        }
//        finally {
//
//        }
//        return fields;
//    }

    protected VEvent createEvent(DateTime startTime, DateTime endTime, String issueKey, String summary) {
        VEvent roomReserveEvent = null;
        if (startTime != null && endTime != null) {
            roomReserveEvent = new VEvent(startTime, endTime, issueKey + " - " + summary);
        }
        else if (endTime != null) {
            roomReserveEvent = new VEvent(endTime, endTime, issueKey + " - " + summary);
        }
        else if (startTime != null) {
            roomReserveEvent = new VEvent(startTime, startTime, issueKey + " - " +summary);
        }
        return roomReserveEvent;
    }

}
