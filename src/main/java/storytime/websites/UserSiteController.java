package storytime.websites;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import storytime.child.Child;
import storytime.child.ChildService;
import storytime.child.StoryPreferences;
import storytime.child.StoryPreferencesService;
import storytime.parent.Parent;
import storytime.parent.ParentService;

import javax.validation.Valid;
import java.util.Optional;

@Controller
@EnableAutoConfiguration
public class UserSiteController {
  private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

  @Autowired
  ParentService parentService;

  @Autowired
  ChildService childService;

  @Autowired
  StoryPreferencesService storyPreferencesService;

  @GetMapping("/")
  public String index() {
    log.info("GET / [website root]");
    return "user/_index";
  }

  /*--- Parent Account ----------------------------------------------------*/
  @GetMapping("/sign-up")
  public String sign_up(Model model) {
    log.info("GET /sign-up");
    model.addAttribute("parent", new Parent());
    return "user/parent-create";
  }

  @PostMapping("/sign-up")
  public String sign_up(@Valid @ModelAttribute("parent") Parent parent, BindingResult result) {

    if (result.hasErrors()) {
      return "user/parent-create";
    }

    log.info("POST /sign-up -- parent");

    // persist will only fail on dupe username?
    if (!parentService.create(parent)) {
      result.rejectValue("username", "error.user", parent.getUsername() + " is already taken.");
      return "user/parent-create";
    }

    String url = "/parent/" + parent.getId();
    log.info("redirecting to: {}", url);

    return "redirect:" + url;
  }

  @GetMapping("/sign-in")
  public String sign_in(Model model) {
    log.info("GET /sign-in");
    model.addAttribute("parent", new Parent());
    return "user/parent-sign-in";
  }

  @PostMapping("/sign-in")
  public String sign_in(@Valid @ModelAttribute("parent") Parent parent, BindingResult result) {

    // check for basic validation fail
    if (result.hasErrors()) {
      return "user/parent-sign-in";
    }

    log.info("POST /sign-in -- parent: {}", parent);

    // attempt to login
    Optional<String> attemptResult = parentService.loginAttempt(parent);
    if (attemptResult.isPresent()) {
      result.reject("sign_in", attemptResult.get());
      return "user/parent-sign-in";
    }

    // lookup the actual parent id for path
    String url = parentService.getIdForUsername(parent.getUsername()).map(i -> "/parent/" + i)
        .orElse("/parent");

    log.info("redirecting to: {}", url);

    return "redirect:" + url;
  }

  @GetMapping("/parent/{id}")
  public String parent__id(Model model, @PathVariable("id") long id) {
    log.info("GET /parent/{}", id);

    // todo should 404 if parent not found
    parentService.read(id).ifPresent(model::addAttribute);

    return "user/parent-home";
  }

  // todo
  @GetMapping("/parent/{id}/edit")
  public String parent__id__edit(Model model, @PathVariable("id") long id) {
    log.info("GET /parent/edit/{}", id);
    model.addAttribute("id", id);
    return "user/parent-edit";
  }

  // todo post mapping parent/id/edit

  /*--- Child & Child Prefs -----------------------------------------------*/
  @GetMapping("/parent/{id}/new-child")
  public String parent__id__new_child(Model model, @PathVariable("id") long id) {
    log.info("GET /parent/{}/new-child", id);

    // todo should 404 if parent not found
    parentService.read(id).ifPresent(model::addAttribute);
    model.addAttribute("child", new Child());

    return "user/child-create";
  }

  @PostMapping("/parent/{id}/new-child")
  public String parent__id__new_child(Model model, @PathVariable("id") long id,
      @Valid @ModelAttribute("child") Child child, BindingResult result) {

    parentService.read(id).ifPresent(child::setParent);

    if (result.hasErrors()) {
      log.info("result: {}", result);
      return "user/child-create";
    }

    log.info("POST /parent/{}/new-child -- child {}", id, child);

    childService.create(child);

    return parent__id(model, id);
  }

  @GetMapping("/parent/{pid}/child/{cid}/prefs")
  public String parent__pid__child__cid__prefs(Model model, @PathVariable("pid") long parentId,
      @PathVariable("cid") long childId) {
    log.info("GET /parent/{}/child/{}/prefs", parentId, childId);

    // todo should 404 if parent or child not found
    parentService.read(parentId).ifPresent(model::addAttribute);
    childService.read(childId).ifPresent(c -> {

      // if child exists add it
      model.addAttribute(c);

      // if child's prefs exist, add them, otherwise add a new one
      storyPreferencesService.getStoryPreferencesForOwner(c).ifPresentOrElse(model::addAttribute,
          () -> model.addAttribute(new StoryPreferences()));
    });

    return "user/child-prefs";
  }

  // todo fix update case
  @PostMapping("/parent/{pid}/child/{cid}/prefs")
  public String parent__pid__child__cid__prefs(Model model, @PathVariable("pid") long parentId,
      @PathVariable("cid") long childId, @ModelAttribute Parent parent, @ModelAttribute Child child,
      @Valid @ModelAttribute StoryPreferences storyPreferences, BindingResult result) {

    if (result.hasErrors()) {
      log.info("result: {}", result);
      parentService.read(parentId).ifPresent(model::addAttribute);
      childService.read(childId).ifPresent(model::addAttribute);

      return "user/child-prefs";
    }
    log.info("POST /parent/{}/child/{}/prefs -- prefs: {}", parentId, childId, storyPreferences);

    // if we got here, child should always exist
    childService.read(childId).ifPresent(storyPreferences::setOwner);

    storyPreferencesService.createOrUpdate(storyPreferences);

    return parent__id(model, parentId);
  }

}
