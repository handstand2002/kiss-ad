function confirmDelete(button) {
  if (confirm("Delete Show?")) {
    button.form.submit();
  }
}