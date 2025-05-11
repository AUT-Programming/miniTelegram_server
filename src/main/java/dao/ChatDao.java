package dao;

import entity.Chat;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

public class ChatDao {
    public void saveChat(Chat chat) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            session.save(chat);
            transaction.commit();

        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public List<Chat> getChatsForUser(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT cm.chat FROM ChatMember cm " +
                                    " WHERE cm.user.username = :uname", Chat.class)
                    .setParameter("uname", username)
                    .list();
        }
    }
}
