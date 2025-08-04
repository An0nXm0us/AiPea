using Microsoft.AspNetCore.Identity;
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
    }
}
