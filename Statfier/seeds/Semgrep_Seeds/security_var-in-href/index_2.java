<!-- cf. https://github.com/caiomartini/mustache-demo/blob/97b9200ebd2d27953febff23e6718aa1aa9ee44d/demo-mustache.html -->
<!DOCTYPE HTML>
<html>

<div class="jumbotron text-center">
    <!-- ok: var-in-href -->
    <a href="#" class="dropdown-toggle" data-toggle="dropdown"> <%= current_user.name.pluralize %> Account <b class="caret"></b></a>
</div>

</html>