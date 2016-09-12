
package src.org.janelia.it.jacs.compute.service.search;

import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 12, 2007
 * Time: 1:01:36 PM
 *
 */
abstract public class AbstractBaseAccessionSearchTest extends AbstractTransactionalDataSourceSpringContextTests  {

    private SessionFactory sessionFactory;

    public AbstractBaseAccessionSearchTest(String name) {
        super(name);
        configureLog4jConsole();
    }

    protected static void configureLog4jConsole() {
        Properties log4jprops = new Properties();
        log4jprops.setProperty("log4j.rootCategory","ALL, stdout");
        log4jprops.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        log4jprops.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        log4jprops.setProperty("log4j.appender.stdout.layout.ConversionPattern","%-5r[%24F:%-3L:%-5p]%x %m%n");
        PropertyConfigurator.configure(log4jprops);
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected String[] getConfigLocations() {
        return new String[] {
                "classpath*:/applicationContext-test.xml",
                "classpath*:/WEB-INF/applicationContext-common.xml"};
    }

    protected Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }


}
