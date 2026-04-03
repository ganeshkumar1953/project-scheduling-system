
// script.js
function showModule(moduleId) {
  // Hide all modules
  document.querySelectorAll('.module').forEach(m => m.style.display = 'none');
  // Show selected module
  document.getElementById(moduleId).style.display = 'block';
}

function showSubmenu(name) {
  // Hide all submenus
  document.querySelectorAll('.submenu').forEach(s => s.style.display = 'none');
  // Show selected submenu
  document.getElementById(name).style.display = 'block';
}// Show registration form
function openRegistrationForm() {
  document.getElementById('registrationForm').style.display = 'block';
}

// Handle form submission
function submitRegistration(event) {
  event.preventDefault(); // Prevent page reload

  const projectName = document.getElementById('projectName').value;
  const members = document.getElementById('members').value;
  const leaderName = document.getElementById('leaderName').value;
  const email = document.getElementById('email').value;
  const description = document.getElementById('description').value;

  alert(
    "Team Registered Successfully!\n\n" +
    "Project: " + projectName + "\n" +
    "Members: " + members + "\n" +
    "Leader: " + leaderName + "\n" +
    "Email: " + email + "\n" +
    "Description: " + description
  );

  // Reset form
  document.querySelector('#registrationForm form').reset();
  document.getElementById('registrationForm').style.display = 'none';
}