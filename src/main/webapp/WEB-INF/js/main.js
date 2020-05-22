$(document).ready(function () {
    $('.nBtn, .table .eBtn').on('click', function (event) {
        event.preventDefault(); // prevent main action

        // get Href attribute that fetches country by id
        var href = $(this).attr('href');
        var text = $(this).text(); // gets the button clicked text

        if (text == "Edit") {
            // Editting Open with Values
            // get request in jquery
            $.get(href, function (country, status) {
                $('.myForm #idForm').show();
                $('.myForm #id').val(country.id);
                // $('.myForm #id').prop( "disabled", true );
                $('.myForm #name').val(country.name);
                $('.myForm #capital').val(country.capital);
            });
            // open modal
            $('.myForm #exampleModal').modal();
        } else {
            // New Request create with Empty Field
           
                $('.myForm #id').val('');
                // $('.myForm #id').prop( "disabled", false );
                // $('.myForm #idForm').hide();
                $('.myForm #name').val('');
                $('.myForm #capital').val('');
            
            // open modal
            $('.myForm #exampleModal').modal();
        }

    });

    // Delete Button
    $('.table .delBtn').on('click', function(event){
        event.preventDefault();
        var href = $(this).attr('href');
        $('#myModal #delRef').attr('href', href);
        $('#myModal').modal();
    });
})