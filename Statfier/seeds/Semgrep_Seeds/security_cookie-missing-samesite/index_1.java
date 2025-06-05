@Controller
public class CookieController {

    @RequestMapping(value = "/cookie2", method = "GET")
    public void setSecureCookie(@RequestParam String value, HttpServletResponse response) {
        // ruleid:cookie-missing-samesite
        response.setHeader("Set-Cookie", "key=value; HttpOnly;");
    }
}