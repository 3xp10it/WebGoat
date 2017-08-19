package org.owasp.webgoat.plugin.challenge7;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.owasp.webgoat.assignments.AssignmentEndpoint;
import org.owasp.webgoat.assignments.AssignmentPath;
import org.owasp.webgoat.assignments.AttackResult;
import org.owasp.webgoat.plugin.SolutionConstants;
import org.owasp.webgoat.users.UserRepository;
import org.owasp.webgoat.users.WebGoatUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static org.owasp.webgoat.plugin.Flag.FLAGS;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author nbaars
 * @since 4/8/17.
 */
@AssignmentPath("/challenge/7")
@Slf4j
public class Assignment7 extends AssignmentEndpoint {

    private static final String TEMPLATE = "Hi, you requested a password reset link, please use this " +
            "<a target='_blank' href='http://localhost:8080/WebGoat/challenge/7/reset-password/%s'>link</a> to reset your password." +
            "\n \n\n" +
            "If you did not request this password change you can ignore this message." +
            "\n" +
            "If you have any comments or questions, please do not hesitate to reach us at support@webgoat-cloud.org" +
            "\n\n" +
            "Kind regards, \nTeam WebGoat";

    @Autowired
    private HazelcastInstance hazelcastInstance;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/reset-password/{link}")
    public ResponseEntity<String> resetPassword(@PathVariable(value = "link") String link) {
        if (link.equals(SolutionConstants.ADMIN_PASSWORD_LINK)) {
            return ResponseEntity.accepted().body("<h1>Success!!</h1>" +
                    "<img src='/WebGoat/images/hi-five-cat.jpg'>" +
                    "<br/><br/>Here is your flag: " + "<b>" + FLAGS.get(7) + "</b>");
        }
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).body("That is not the reset link for admin");
    }

    @RequestMapping(method = POST)
    @ResponseBody
    public AttackResult sendPasswordResetLink(@RequestParam String email) {
        if (StringUtils.hasText(email)) {
            WebGoatUser webGoatUser = userRepository.findByUsername(email.substring(0, email.indexOf("@")));
            if (webGoatUser != null) {
                String username = webGoatUser.getUsername();
                IMap<Object, Object> emails = hazelcastInstance.getMap("usersMail");
                Mailbox mailbox = new Mailbox();
                mailbox = (Mailbox) emails.getOrDefault(username, mailbox);
                mailbox.addMail(Mailbox.Email.builder()
                        .title("Your password reset link for challenge 7")
                        .contents(String.format(TEMPLATE, new PasswordResetLink().createPasswordReset(username, "webgoat")))
                        .sender("password-reset@webgoat-cloud.net")
                        .time(LocalDateTime.now()).build());
                emails.put(username, mailbox);
            }
        }
        return success().feedback("email.send").feedbackArgs(email).build();
    }

    @RequestMapping(method = GET, value="/.git", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    @SneakyThrows
    public ClassPathResource git() {
        return new ClassPathResource("challenge7/git.zip");
    }
}

