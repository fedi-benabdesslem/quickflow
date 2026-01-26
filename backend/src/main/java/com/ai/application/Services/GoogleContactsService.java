package com.ai.application.Services;

import com.ai.application.model.Entity.Contact;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service for fetching contacts from Google People API.
 */
@Service
public class GoogleContactsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleContactsService.class);
    private static final String APPLICATION_NAME = "QuickFlow";
    private static final int PAGE_SIZE = 1000;

    /**
     * Fetch all contacts from Google account using the user's access token.
     * 
     * @param accessToken The user's Google OAuth access token
     * @return List of Contact objects
     */
    public List<Contact> fetchContacts(String accessToken) throws IOException, GeneralSecurityException {
        logger.info("Fetching contacts from Google People API");

        List<Contact> contacts = new ArrayList<>();

        try {
            // Create credentials from access token
            GoogleCredentials credentials = GoogleCredentials.create(
                    new AccessToken(accessToken, new Date(System.currentTimeMillis() + 3600000)));

            // Build HTTP transport
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            // Create People service
            PeopleService peopleService = new PeopleService.Builder(
                    httpTransport,
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Fetch contacts with pagination
            String pageToken = null;
            do {
                ListConnectionsResponse response = peopleService.people().connections()
                        .list("people/me")
                        .setPageSize(PAGE_SIZE)
                        .setPersonFields("names,emailAddresses,phoneNumbers,photos")
                        .setPageToken(pageToken)
                        .execute();

                List<Person> connections = response.getConnections();
                if (connections != null) {
                    for (Person person : connections) {
                        Contact contact = personToContact(person);
                        if (contact != null && contact.getEmail() != null) {
                            contacts.add(contact);
                        }
                    }
                }

                pageToken = response.getNextPageToken();
            } while (pageToken != null);

            logger.info("Fetched {} contacts from Google", contacts.size());

        } catch (Exception e) {
            logger.error("Error fetching contacts from Google: {}", e.getMessage(), e);
            throw e;
        }

        return contacts;
    }

    /**
     * Convert Google Person to Contact entity.
     */
    private Contact personToContact(Person person) {
        Contact contact = new Contact();

        // Get resource name as source ID
        String resourceName = person.getResourceName();
        if (resourceName != null) {
            contact.setSourceId(resourceName);
        }

        // Get name
        List<Name> names = person.getNames();
        if (names != null && !names.isEmpty()) {
            contact.setName(names.get(0).getDisplayName());
        }

        // Get primary email
        List<EmailAddress> emails = person.getEmailAddresses();
        if (emails != null && !emails.isEmpty()) {
            contact.setEmail(emails.get(0).getValue());
        }

        // Get phone
        List<PhoneNumber> phones = person.getPhoneNumbers();
        if (phones != null && !phones.isEmpty()) {
            contact.setPhone(phones.get(0).getValue());
        }

        // Get photo
        List<Photo> photos = person.getPhotos();
        if (photos != null && !photos.isEmpty()) {
            String photoUrl = photos.get(0).getUrl();
            if (photoUrl != null && !photoUrl.contains("default-user")) {
                contact.setPhoto(photoUrl);
            }
        }

        // Set source
        contact.setSource("google");

        return contact;
    }
}
