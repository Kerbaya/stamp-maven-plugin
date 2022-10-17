import com.kerbaya.stamp.PomUtils

PomUtils.assertBuildNumber("1", 1L, basedir);
PomUtils.match("1-\\d{8}\\.\\d{6}-1", "/pom:project/pom:parent/pom:version[text()]", basedir, "child");
