@Controller
public class CookieController {

    @RequestMapping(value = "/cookie1", method = "GET")
    public void setCookie(@RequestParam String value, HttpServletResponse response) {
        // ok:cookie-missing-samesite
        response.setHeader("Set-Cookie", "key=value; HttpOnly; SameSite=strict");
    }
}