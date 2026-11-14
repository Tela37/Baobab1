package td.teladoum.baobab.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import td.teladoum.baobab.models.User;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    public int id;
    public String name;
    public String phone;
    public String email;
    public String profileImage;
    public String gender;
    public String dob;
    public String city;
    public String country;
    public boolean isOnline;
    public String lastSeen;
    public boolean isSuspended;
    public boolean isAdmin;

    public UserEntity() {}

    public static UserEntity fromModel(User user) {
        if (user == null) return null;
        UserEntity entity = new UserEntity();
        entity.id = user.getId();
        entity.name = user.getName();
        entity.phone = user.getPhone();
        entity.email = user.getEmail();
        entity.profileImage = user.getProfileImage();
        entity.gender = user.getGender();
        entity.dob = user.getDob();
        entity.city = user.getCity();
        entity.country = user.getCountry();
        entity.isOnline = user.isOnline();
        entity.lastSeen = user.getLastSeen();
        entity.isSuspended = user.isSuspended();
        entity.isAdmin = user.isAdmin();
        return entity;
    }

    public User toModel() {
        User u = new User(id, name, phone, email, null, profileImage);
        u.setGender(gender);
        u.setDob(dob);
        u.setCity(city);
        u.setCountry(country);
        u.setOnline(isOnline);
        u.setLastSeen(lastSeen);
        u.setSuspended(isSuspended);
        u.setAdmin(isAdmin);
        return u;
    }
}
