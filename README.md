Here is a Clojure library to produce iCalendar objects, as defined by
RFC 2445.

    => (use 'clj-ical.format)

Create a VCALENDAR with some VEVENTS

    => (write-object 
         [:vcalendar 
           [:vevent 
             [:summary "Bastille Day Party"]]])

will produce

    BEGIN:VCALENDAR
    BEGIN:VEVENT
    SUMMARY:Bastille Day Party
    END:VEVENT
    END:VCALENDAR

The library correctly obeys all requirements of the RFC 2445
specification, such as 

* long-line folding
* property parameters
* CRLF line termination


