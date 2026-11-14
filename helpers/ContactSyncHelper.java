package td.teladoum.baobab.helpers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.UserRepository;

/**
 * Helper de lecture et de correspondance des contacts du répertoire téléphonique.
 * Permet d'identifier les contacts installés sur BaobabTD (style WhatsApp)
 * et de proposer l'invitation pour les autres.
 */
public class ContactSyncHelper {

    public static class PhoneContactItem {
        public String contactName;
        public String phoneNumber;
        public String cleanDigits;
        public User matchedUser;
        public boolean isRegisteredOnApp;

        public PhoneContactItem(String contactName, String phoneNumber, String cleanDigits) {
            this.contactName = contactName;
            this.phoneNumber = phoneNumber;
            this.cleanDigits = cleanDigits;
            this.isRegisteredOnApp = false;
        }
    }

    public static List<PhoneContactItem> loadPhonebookContacts(Context context) {
        List<PhoneContactItem> contactList = new ArrayList<>();
        if (context == null) return contactList;

        try {
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null) {
                Map<String, Boolean> seenNumbers = new HashMap<>();
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));

                    if (number != null && !number.trim().isEmpty()) {
                        String cleanDigits = UserRepository.cleanPhone(number);
                        if (!cleanDigits.isEmpty() && !seenNumbers.containsKey(cleanDigits)) {
                            seenNumbers.put(cleanDigits, true);
                            PhoneContactItem item = new PhoneContactItem(
                                    name != null ? name : number,
                                    number,
                                    cleanDigits
                            );
                            contactList.add(item);
                        }
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return contactList;
    }

    public static List<PhoneContactItem> matchContactsWithAppUsers(Context context, int currentUserId) {
        List<PhoneContactItem> allContacts = loadPhonebookContacts(context);
        if (context == null) return allContacts;

        UserRepository userRepository = new UserRepository(context);
        List<User> appUsers = userRepository.getAllUsers();

        Map<String, User> userPhoneMap = new HashMap<>();
        for (User u : appUsers) {
            if (u.getId() != currentUserId && u.getPhone() != null && !u.getPhone().trim().isEmpty()) {
                String clean = UserRepository.cleanPhone(u.getPhone());
                if (!clean.isEmpty()) {
                    userPhoneMap.put(clean, u);
                }
            }
        }

        for (PhoneContactItem item : allContacts) {
            if (userPhoneMap.containsKey(item.cleanDigits)) {
                item.matchedUser = userPhoneMap.get(item.cleanDigits);
                item.isRegisteredOnApp = true;
            }
        }

        return allContacts;
    }
}
