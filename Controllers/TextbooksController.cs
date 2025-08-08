using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using Microsoft.EntityFrameworkCore;
using WebApplication3.Data;
using WebApplication3.Models;

namespace WebApplication3.Controllers
{
    public class TextbooksController : Controller
    {
        private readonly ApplicationDbContext _context;

        public TextbooksController(ApplicationDbContext context)
        {
            _context = context;
        }

        // GET: Textbooks
        public async Task<IActionResult> Index()
        {
            var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;

            if (userId == null)
            {
                return Unauthorized();
            }

            var textbooks = await _context.Textbooks .Where(t => t.id == userId).ToListAsync();
            return View(textbooks);
        }


        // GET: Textbooks/Details/5
        public async Task<IActionResult> Details(int? id)
        {
            if (id == null)
            {
                return NotFound();
            }

            var textbook = await _context.Textbooks
                .FirstOrDefaultAsync(m => m.textbookId == id);
            if (textbook == null)
            {
                return NotFound();
            }

            return View(textbook);
        }

        // GET: Textbooks/Create
        public IActionResult Create()
        {
            return View();
        }

        // POST: Textbooks/Create
        // To protect from overposting attacks, enable the specific properties you want to bind to.
        // For more details, see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Create([Bind("textbookName,textbookModuleCode,textbookAuthor")] Textbook textbook)
        {
            if (ModelState.IsValid)
            {
                Random random = new Random();
                textbook.textbookId = random.Next(100000, 999999); // Adjust range as needed

                var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
                if (userId == null)
                {
                    return Unauthorized(); // Or redirect to login
                }
                textbook.id = userId;
                _context.Add(textbook);
                await _context.SaveChangesAsync();
                return RedirectToAction(nameof(Index));
            }

            return View(textbook);
        }


        // GET: Textbooks/Edit/5
        public async Task<IActionResult> Edit(int? id)
        {
            if (id == null)
            {
                return NotFound();
            }

            var textbook = await _context.Textbooks.FindAsync(id);
            if (textbook == null)
            {
                return NotFound();
            }
            return View(textbook);
        }

        // POST: Textbooks/Edit/5
        // To protect from overposting attacks, enable the specific properties you want to bind to.
        // For more details, see http://go.microsoft.com/fwlink/?LinkId=317598.
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Edit(int id, [Bind("textbookId,textbookName,textbookModuleCode,textbookAuthor,id")] Textbook textbook)
        {
            if (id != textbook.textbookId)
            {
                return NotFound();
            }

            if (ModelState.IsValid)
            {
                try
                {
                    _context.Update(textbook);
                    await _context.SaveChangesAsync();
                }
                catch (DbUpdateConcurrencyException)
                {
                    if (!TextbookExists(textbook.textbookId))
                    {
                        return NotFound();
                    }
                    else
                    {
                        throw;
                    }
                }
                return RedirectToAction(nameof(Index));
            }
            return View(textbook);
        }

        // GET: Textbooks/Delete/5
        public async Task<IActionResult> Delete(int? id)
        {
            if (id == null)
            {
                return NotFound();
            }

            var textbook = await _context.Textbooks
                .FirstOrDefaultAsync(m => m.textbookId == id);
            if (textbook == null)
            {
                return NotFound();
            }

            return View(textbook);
        }

        // POST: Textbooks/Delete/5
        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteConfirmed(int id)
        {
            var textbook = await _context.Textbooks.FindAsync(id);
            if (textbook != null)
            {
                _context.Textbooks.Remove(textbook);
            }

            await _context.SaveChangesAsync();
            return RedirectToAction(nameof(Index));
        }

        private bool TextbookExists(int id)
        {
            return _context.Textbooks.Any(e => e.textbookId == id);
        }
    }
}
