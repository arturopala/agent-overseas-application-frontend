$(function () {
    //Accessibility
    var errorSummary = $('#error-summary-display'),
        $input = $('input:text');

    //Error summary focus
    if (errorSummary) {
        errorSummary.focus()
    }

    $input.each(function () {
        if ($(this).closest('label').hasClass('form-field--error')) {
            $(this).attr('aria-invalid', true)
        } else {
            $(this).attr('aria-invalid', false)
        }
    });

    //Trim inputs and Capitalize postode
    $('[type="submit"]').click(function () {
        $input.each(function () {
            if ($(this).val() && $(this).attr('data-uppercase') === 'true') {
                $(this).val($(this).val().toUpperCase().replace(/\s\s+/g, ' ').trim())
            } else {
                $(this).val($(this).val().trim())
            }
        });
    });

    //Add aria-hidden to hidden inputs
    $('[type="hidden"]').attr("aria-hidden", true)

    var showHideContent = new GOVUK.ShowHideContent()
    showHideContent.init()


    $('.form-date label.form-field--error').each(function () {

        $(this).closest('div').addClass('form-field--error')
        var $relocate = $(this).closest('fieldset').find('legend')
        $(this).find('.error-notification').appendTo($relocate)

    });

    $('body').on('change', '#country-auto-complete', function () {
        if (!$(this).val()) {
            $('#country select option').removeAttr('selected')
        }

    });

    var selectCountryEl = document.querySelector('#country-auto-complete')
    if (selectCountryEl) {
        accessibleAutocomplete.enhanceSelectElement({
            autoselect: true,
            defaultValue: selectCountryEl.options[selectCountryEl.options.selectedIndex].innerHTML,
            minLength: 2,
            selectElement: selectCountryEl
        })
    }

    function findCountry(country) {
        return country == $("#country-auto-complete").val();
    }

    //custom handle for not found countries
    $('#country-auto-complete').change(function () {
        var changedValue = $(this).val()
        var array = [];

        $('.autocomplete__menu li').each(function () {
            array.push($(this).text())
        })

        if (array == "No results found") {
            $('#country-auto-complete-select').append('<option id="notFound" value="NOTFOUND">No results found</option>')
            $('#country-auto-complete-select').val('NOTFOUND').attr("selected", "selected");

        } else if (array == "") {
            $('#country-auto-complete-select').val('').attr("selected", "selected");
        }

    });


    var selectAmlsEl = document.querySelector('#amls-auto-complete')

    if (selectAmlsEl) {
        accessibleAutocomplete.enhanceSelectElement({
            autoselect: true,
            defaultValue: selectAmlsEl.options[selectAmlsEl.options.selectedIndex].innerHTML,
            minLength: 2,
            selectElement: selectAmlsEl
        })
    }

    //custom handler for AMLS auto-complete dropdown
    $('#amls-auto-complete').change(function () {
        var changedValue = $(this).val()
        var array = [];

        $('.autocomplete__menu li').each(function () {
            array.push($(this).text())
        })

        if (array == "No results found") {
            $('#amls-auto-complete-select').append('<option id="notFound" value="NOTFOUND">No results found</option>')
            $('#amls-auto-complete-select').val('NOTFOUND').attr("selected", "selected");

        } else if (array == "") {
            $('#amls-auto-complete-select').val('').attr("selected", "selected");
        }

    });

    $('.form-date label.form-field--error').each(function () {

        $(this).closest('div').addClass('form-field--error')
        var $relocate = $(this).closest('fieldset').find('legend')
        $(this).find('.error-notification').appendTo($relocate)

    });

    //by default the amlsForm will be hidden so we we need this to make the form visible after loaded
    $('#amlsForm').css('visibility', 'visible');

    GOVUK.details.init();


});

$(document).ready(function () {

    // File upload

    if (document.getElementById('govuk-box')) {
        var loader = new GOVUK.Loader();
        loader.init({
            container: 'govuk-box'
        })
    }

    if (document.getElementById('file-upload-loading')) {
        var loader2 = new GOVUK.Loader();
        loader2.init({
            container: 'file-upload-loading',
            label: true,
            labelText: 'This file is being checked and uploaded',
        })
    }

    $('.file-upload').on('click', function (e) {
        var loadingSection = $('.loader'),
            uploadFormElements = $('.hide-when-uploading');

        uploadFormElements.hide();
        loadingSection.show();
        $("html, body").animate({ scrollTop: 0 });
        pollUploadStatus();
    });

    var statusPollCount= {};
    statusPollCount.timer = 0;

    var pollUploadStatus = function () {
        var fileReference = $('.file-upload').data('reference'),
            fileType = $('.file-upload').data('filetype'),
            baseUrl = "/agent-services/apply-from-outside-uk",
            pollUrl = "/poll-status/" + fileType + "/";

        setTimeout(function () {
            $.ajax({
                url: baseUrl + pollUrl + fileReference,
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                type: "GET",
                success: function (data) {
                    if (data) {
                        if (data.fileStatus === 'READY') {
                            window.location.href = baseUrl + "/file-uploaded-successfully";
                        } else if (data.fileStatus === 'FAILURE') {
                            window.location.href = baseUrl + "/file-upload-failed";
                        } else if (data.fileStatus === 'NOT_READY') {
                            statusPollCount.timer++;
                            if(statusPollCount.timer === 30){
                                window.location.href = baseUrl + "/file-upload-failed";
                            }
                        } else {
                            window.location.href = baseUrl + "/file-upload-failed";
                        }
                    }
                },
                dataType: "json",
                complete: pollUploadStatus
            })
        }, 500);
    }

    $('#file-upload').change(function (e) {
        var fileName = e.target.files[0].name;

        if ((fileName.indexOf('pdf') === -1) && (fileName.indexOf('jpeg') === -1) && (fileName.indexOf('jpg') === -1)) {
            window.location.href = baseUrl + "/file-upload-failed"
        }
    });

});