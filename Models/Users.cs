<<<<<<< HEAD
﻿using Microsoft.AspNetCore.Identity;
using Microsoft.VisualBasic;
using System.ComponentModel.DataAnnotations;

namespace Eddie.Models
{
    //This is an extension of the user details.
    public class Users: IdentityUser
    {
        [Required]
        public string FirstName {  get; set; }

        [Required]
        public string LastName { get; set; }

        [Required]
        public DateTime CreatedAt { get; set; }

        [Required]
        public string Role {  get; set; }


        [Required]
        public string Institution { get; set; }
=======
﻿using System.ComponentModel.DataAnnotations;

namespace WebApplication3.Models
{
    public class Users
    {
        public class User
        {
            //reresent table (macro code, 2024)
            [Required]
            [Key]
            public string UserId { get; set; }

            [Required(ErrorMessage = "UserName required")]
            [MaxLength(50, ErrorMessage = "50 characters Max")]
            public string FullName { get; set; }

            [Required(ErrorMessage = "Password required")]
            [MaxLength(200, ErrorMessage = "80 characters Min Max is 200")]
            public string Password { get; set; }

            [Required(ErrorMessage = "Email required")]
            [MaxLength(100, ErrorMessage = "100 characters Max")]
            public string Email { get; set; }
        }
>>>>>>> c9d7f6c (Initial commit)
    }
}
