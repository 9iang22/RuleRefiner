<h4>From: {{ from_email }}</h4>
<h4>To:
    {% for recipient in recipients %}
    {{ recipient }}&nbsp;
    {% endfor %}
</h4>
<h4>Subject: {{subject}}</h4>
<div class="email" style="display: block;">
    {{ message }}
</div>
<div class="email-text" style="display: none;">
    <pre>{{ body }}</pre>
    <!-- ruleid: template-href-var -->
    <a href = '{{ link }}' >{{ link_text }}</a>
</div>
<hr>