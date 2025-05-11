package dao;

import entity.ChatMember;
import entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;

public class ChatMemberDao {
    public User findUserByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM User u WHERE u.username = :uname", User.class)
                    .setParameter("uname", username)
                    .uniqueResult();
        }
    }
    public void save(ChatMember chatMember) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(chatMember);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public boolean chatMemberExists(Long chatId, Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(*) FROM ChatMember WHERE chat.id = :chatId AND user.id = :userId";
            Long count = (Long) session.createQuery(hql)
                    .setParameter("chatId", chatId)
                    .setParameter("userId", userId)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public boolean isMember(Long chatId, Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(cm) FROM ChatMember cm " +
                                    " WHERE cm.chat.id = :chatId AND cm.user.id = :userId", Long.class)
                    .setParameter("chatId", chatId)
                    .setParameter("userId", userId)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }
}
