package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import mflix.api.models.Session;
import mflix.api.models.User;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.text.MessageFormat;
import java.util.Map;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class UserDao extends AbstractMFlixDao {

  private final MongoCollection<User> usersCollection;
  private final MongoCollection<Session> sessionsCollection;

  private final Logger log;

  @Autowired
  public UserDao(
      MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
    super(mongoClient, databaseName);
    CodecRegistry pojoCodecRegistry =
        fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    usersCollection = db.getCollection("users", User.class).withCodecRegistry(pojoCodecRegistry);
    sessionsCollection = db.getCollection("sessions", Session.class).withCodecRegistry(pojoCodecRegistry);
    log = LoggerFactory.getLogger(this.getClass());
  }

  /**
   * Inserts the `user` object in the `users` collection.
   *
   * @param user - User object to be added
   * @return True if successful, throw IncorrectDaoOperation otherwise
   */
  public boolean addUser(User user) {
    //TODO > Ticket: Durable Writes -  you might want to use a more durable write concern here!
    usersCollection.insertOne(user);
    return true;
    //TODO > Ticket: Handling Errors - make sure to only add new users
    // and not users that already exist.

  }

  /**
   * Creates session using userId and jwt token.
   *
   * @param userId - user string identifier
   * @param jwt - jwt string token
   * @return true if successful
   */
  public boolean createUserSession(String userId, String jwt) {
    Document user = new Document();
    user.put("user_id", userId);
    user.put("jwt", jwt);

    sessionsCollection.updateOne(Filters.eq("user_id", userId), new Document("$set", user), new UpdateOptions().upsert(true));
    return true;

    //TODO > Ticket: Handling Errors - implement a safeguard against
    // creating a session with the same jwt token.
  }

  /**
   * Returns the User object matching the an email string value.
   *
   * @param email - email string to be matched.
   * @return User object or null.
   */
  public User getUser(String email) {
    User user = usersCollection.find(Filters.eq("email", email)).first();

    return user;
  }

  /**
   * Given the userId, returns a Session object.
   *
   * @param userId - user string identifier.
   * @return Session object or null.
   */
  public Session getUserSession(String userId) {
    return sessionsCollection.find(Filters.eq("user_id", userId)).first();
  }

  public boolean deleteUserSessions(String userId) {
    sessionsCollection.deleteOne(Filters.eq("user_id", userId));

    return true;
  }

  /**
   * Removes the user document that match the provided email.
   *
   * @param email - of the user to be deleted.
   * @return true if user successfully removed
   */
  public boolean deleteUser(String email) {
    // remove user sessions
    deleteUserSessions(email);

    // remove user
    usersCollection.deleteOne(Filters.eq("email", email));

    //TODO > Ticket: Handling Errors - make this method more robust by
    // handling potential exceptions.

    return true;
  }

  /**
   * Updates the preferences of an user identified by `email` parameter.
   *
   * @param email - user to be updated email
   * @param userPreferences - set of preferences that should be stored and replace the existing
   *     ones. Cannot be set to null value
   * @return User object that just been updated.
   */
  public boolean updateUserPreferences(String email, Map<String, ?> userPreferences) {
    if (userPreferences == null) {
      throw new IncorrectDaoOperation("Preferences cannot be empty");
    }

    usersCollection.updateOne(Filters.eq("email", email), new Document("$set", new Document("preferences", userPreferences)));

    //TODO > Ticket: Handling Errors - make this method more robust by
    // handling potential exceptions when updating an entry.
    return true;
  }
}
