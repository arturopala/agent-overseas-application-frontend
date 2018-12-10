$(function() {
    //Accessibility
    var errorSummary =  $('#error-summary-display'),
    $input = $('input:text')
    //Error summary focus
    if (errorSummary){ errorSummary.focus() }
    $input.each( function(){
        if($(this).closest('label').hasClass('form-field--error')){
            $(this).attr('aria-invalid', true)
        }else{
            $(this).attr('aria-invalid', false)
        }
    });
    //Trim inputs and Capitalize postode
    $('[type="submit"]').click(function(){
        $input.each( function(){
            if($(this).val() && $(this).attr('data-uppercase') === 'true' ){
                $(this).val($(this).val().toUpperCase().replace(/\s\s+/g, ' ').trim())
            }else{
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

        })


      var showHideContent = new GOVUK.ShowHideContent()
      showHideContent.init()

        $('body').on('change', '#country-auto-complete', function(){
           if(!$(this).val()){
                       $('#country select option').removeAttr('selected')
                  }

        });

       var selectEl = document.querySelector('#country-auto-complete')
          if(selectEl){
              accessibleAutocomplete.enhanceSelectElement({
                autoselect: true,
                defaultValue: selectEl.options[selectEl.options.selectedIndex].innerHTML,
                minLength: 2,
                selectElement: selectEl
              })
          }

          function findCountry(country) {
              return  country == $("#country-auto-complete").val();
          }

        //custom handle for not found countries
        $('#country-auto-complete').change(function(){
            var changedValue = $(this).val()
            var array = [];

            $('.autocomplete__menu li').each(function(){
                array.push($(this).text())
            })

            if(array == "No results found"){
                $('#country-auto-complete-select').append('<option id="notFound" value="NOTFOUND">No results found</option>')
                $('#country-auto-complete-select').val('NOTFOUND').attr("selected", "selected");

            }else if(array == ""){
                $('#country-auto-complete-select').val('').attr("selected", "selected");
            }

        });

});
