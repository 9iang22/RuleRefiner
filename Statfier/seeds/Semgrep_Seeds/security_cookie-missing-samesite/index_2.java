@Controller
public class CookieController {

    @RequestMapping(value = "/cookie3", method = "GET")
    public void setSecureHttponlyCookie(@RequestParam String value, HttpServletResponse response) {
        Cookie cookie = new Cookie("cookie", value);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        // ruleid:cookie-missing-samesite
        response.addCookie(cookie);
    }
}