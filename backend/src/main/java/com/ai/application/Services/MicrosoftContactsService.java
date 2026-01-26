package com.ai.application.Services;

import com.ai.application.model.Entity.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for fetching contacts from Microsoft Graph API.
 */
@Service
public class MicrosoftContactsService {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftContactsService.class);
    private static final String GRAPH_API_BASE = "https://graph.microsoft.com/v1.0";
    private static final int PAGE_SIZE = 999;

    private final RestTemplate restTemplate;

    public MicrosoftContactsService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetch all contacts from Microsoft account using the user's access token.
     * 
     * @param accessToken The user's Microsoft OAuth access token
     * @return List of Contact objects
     */
    @SuppressWarnings("unchecked")
    public List<Contact> fetchContacts(String accessToken) {
        logger.info("Fetching contacts from Microsoft Graph API");

        List<Contact> contacts = new ArrayList<>();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = GRAPH_API_BASE + "/me/contacts?$top=" + PAGE_SIZE +
                    "&$select=id,displayName,emailAddresses,mobilePhone";

            // Fetch contacts with pagination
            while (url != null) {
                ResponseEntity<Map> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Map.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();

                    List<Map<String, Object>> values = (List<Map<String, Object>>) body.get("value");
                    if (values != null) {
                        for (Map<String, Object> contactData : values) {
                            Contact contact = mapToContact(contactData);
                            if (contact != null && contact.getEmail() != null) {
                                contacts.add(contact);
                            }
                        }
                    }

                    // Check for next page
                    url = (String) body.get("@odata.nextLink");
                } else {
                    url = null;
                }
            }

            logger.info("Fetched {} contacts from Microsoft", contacts.size());

        } catch (Exception e) {
            logger.error("Error fetching contacts from Microsoft: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Microsoft contacts: " + e.getMessage(), e);
        }

        return contacts;
    }

    /**
     * Convert Microsoft Graph contact to Contact entity.
     */
    @SuppressWarnings("unchecked")
    private Contact mapToContact(Map<String, Object> data) {
        Contact contact = new Contact();

        // Get ID as source ID
        String id = (String) data.get("id");
        if (id != null) {
            contact.setSourceId(id);
        }

        // Get display name
        String displayName = (String) data.get("displayName");
        contact.setName(displayName);

        // Get primary email
        List<Map<String, Object>> emailAddresses = (List<Map<String, Object>>) data.get("emailAddresses");
        if (emailAddresses != null && !emailAddresses.isEmpty()) {
            String email = (String) emailAddresses.get(0).get("address");
            contact.setEmail(email);
        }

        // Get mobile phone
        String mobilePhone = (String) data.get("mobilePhone");
        contact.setPhone(mobilePhone);

        // Microsoft Graph doesn't return photos in the contacts list
        // Would need a separate call to /me/contacts/{id}/photo/$value
        contact.setPhoto(null);

        // Set source
        contact.setSource("microsoft");

        return contact;
    }

    /**
     * Fetch photo for a specific contact.
     * Returns the photo URL or null if not available.
     */
    public String fetchContactPhoto(String accessToken, String contactId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            String url = GRAPH_API_BASE + "/me/contacts/" + contactId + "/photo/$value";

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Convert to base64 data URL
                String base64 = java.util.Base64.getEncoder().encodeToString(response.getBody());
                return "data:image/jpeg;base64," + base64;
            }
        } catch (Exception e) {
            // Photo not available is common, don't log as error
            logger.debug("No photo available for contact {}", contactId);
        }

        return null;
    }
}
