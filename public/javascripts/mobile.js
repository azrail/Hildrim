$('div[id="checkins"] ul[data-role="listview"] a').live("click", function () {
    var a = $(this).attr("data-url");
    a != null && $.mobile.changePage(a)
});

$('div[id="checkins"]').live("pageshow", function () {
    $('div[id="checkins"] ul[data-role="listview"]').children(0).text() == "Loading.." && $.ajax({
        type: "GET",
        url: "/service/checkins",
        data: "{}",
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (a) {
            $.mobile.pageLoading();
            $('div[id="checkins"] ul[data-role="listview"]').html("");
            var c = "";
            $(a.results).each(function (b) {
                c += "<li role='option' tabindex='-1' data-theme='c'><a data-identity='" + a.results[b].checkin_id + "' data-url='/media/checkin/" + a.results[b].checkin_id + "' href='javascript:void();'>" + a.results[b].title + "</a></li>"
            });
            $('div[id="checkins"] ul[data-role="listview"]').html(c);
            $('div[id="checkins"] ul[data-role="listview"]').listview("refresh");
            $.mobile.pageLoading(true)
        },
        error: function () {
            $('div[data-url="checkins"] div[data-role="content"]').html("Error loading checkins. Please try web version.")
        }
    })
});