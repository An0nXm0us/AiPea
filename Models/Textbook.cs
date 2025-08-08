using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace WebApplication3.Models
{
    public class Textbook
    {
        [Required]
        [Key]
        public int textbookId { get; set; }
        [MaxLength(40)]
        [Required]
        public string textbookName { get; set; }
        [MaxLength(8)]
        [Required]
        public string textbookModuleCode { get; set; }
        [MaxLength(200)]
        [Required]
        public string textbookAuthor { get; set; }
        
        [ForeignKey("UserId")]
        [Required]
        public string id { get; set; }
        public virtual IdentityUser User { get; set; }

    }
}
