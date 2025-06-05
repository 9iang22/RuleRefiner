@Controller
public class CookieController {

    @RequestMapping(value = "/cookie4", method = "GET")
    public void setEverything(@RequestParam String value, HttpServletResponse response) {
        // ok:cookie-missing-samesite
        response.setHeader("Set-Cookie", "key=value; HttpOnly; Secure; SameSite=strict");
        response.addCookie(cookie);
    }
}