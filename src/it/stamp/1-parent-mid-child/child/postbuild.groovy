import com.kerbaya.preserve.PomUtils

/*
 * even though parent qualifies for stamping, we didn't start execution in the parent folder (we
 * started in the child folder).  so leave parent pom.xml alone
 */
PomUtils.assertVersion("1-SNAPSHOT", basedir, "..");
PomUtils.match("1-SNAPSHOT", "/pom:project/pom:parent/pom:version[text()]", basedir);

PomUtils.assertBuildNumber("2", 1L, basedir);
