<<<<<<< HEAD
﻿using Eddie.Models;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace Eddie.Data
{
    public class ApplicationDbContext : IdentityDbContext<Users>
=======
﻿using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using WebApplication3.Models;
using static WebApplication3.Models.Users;

namespace WebApplication3.Data
{
    public class ApplicationDbContext : IdentityDbContext
>>>>>>> c9d7f6c (Initial commit)
    {
        public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
            : base(options)
        {
<<<<<<< HEAD

        }
=======
        }
        public DbSet<User> Users { get; set; }
        public DbSet<Textbook> Textbooks { get; set; }
>>>>>>> c9d7f6c (Initial commit)
    }
}
