function deriveShowName(urlField) {
  var currentUrl = urlField.value;
  currentUrl = trimAfterLastInstance(currentUrl, "#");
  currentUrl = trimTrailing(currentUrl, "/");
  var showName = getTrailingPath(currentUrl);
  showName = capitalizeEachWord(showName);

  urlField.form.title.value=showName;
  urlField.form.folderName.value=showName;
  urlField.value=currentUrl;

  var isSubsPlease = urlField.value.match(/subsplease/i) != null;
  if (isSubsPlease) {
    urlField.form.sourceName.value = "SUBSPLEASE";
  }
}

function trimAfterLastInstance(str, character) {
  if (str.indexOf(character) >=0 ) {
    return str.substr(0, str.lastIndexOf(character));
  } else {
    return str
  }
}

function trimTrailing(str, trailingChar) {
  while (str.substr(str.length-1, str.length)==trailingChar) {
    str = str.substr(0, str.length-1);
  }
  return str;
}

function getTrailingPath(str) {
  return str.substr(str.lastIndexOf("/")+1);
}

function capitalizeEachWord(str) {
  return str.split("-").flatMap(word => word.substr(0,1).toUpperCase()+word.substr(1)).join(" ")
}

function autofillCronTime(field) {
  var today = new Date();
  var cron = "0 ";
  var cronMinutes = today.getMinutes() < 30 ? 30 : 0;

  var cronHours = today.getHours();
  var cronDay = today.getDay();
  if (cronMinutes == 0) {
    cronHours = (cronHours + 1) % 24;
    if (cronHours == 0) {
      cronDay = (cronDay + 1) % 7;
    }
  }
  var cronDayOfWeek;
  switch(cronDay) {
    case 0: cronDayOfWeek="SUN";
    break;
    case 1: cronDayOfWeek="MON";
    break;
    case 2: cronDayOfWeek="TUE";
    break;
    case 3: cronDayOfWeek="WED";
    break;
    case 4: cronDayOfWeek="THU";
    break;
    case 5: cronDayOfWeek="FRI";
    break;
    case 6: cronDayOfWeek="SAT";
    break;
  }

  field.value = `0 ${cronMinutes} ${cronHours} * * ${cronDayOfWeek}`;
}
window.onload = function() {
 autofillCronTime(document.getElementsByTagName("form")[0].releaseScheduleCron)
}
